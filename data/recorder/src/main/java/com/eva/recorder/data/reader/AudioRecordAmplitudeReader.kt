package com.eva.recorder.data.reader

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.eva.datastore.domain.repository.RecorderAudioSettingsRepo
import com.eva.recorder.data.ext.normalize
import com.eva.recorder.data.ext.padListWithExtra
import com.eva.recorder.data.ext.rms
import com.eva.recorder.data.ext.smoothen
import com.eva.recorder.data.ext.toProperSequence
import com.eva.recorder.domain.models.RecordedPoint
import com.eva.recorder.domain.models.RecorderState
import com.eva.recorder.domain.stopwatch.RecorderStopWatch
import com.eva.utils.RecorderConstants
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration

private const val TAG = "AmplitudeVisualizer"

@OptIn(
	ExperimentalAtomicApi::class,
	ExperimentalCoroutinesApi::class
)
@SuppressLint("MissingPermission")
class AudioRecordAmplitudeReader(
	private val context: Context,
	private val stopWatch: RecorderStopWatch,
	private val settings: RecorderAudioSettingsRepo,
	private val delayRate: Duration = RecorderConstants.AMPS_READ_DELAY_RATE,
	private val bufferSize: Int = RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE,
) {

	private val _hasRecordPermission: Boolean
		get() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
				PermissionChecker.PERMISSION_GRANTED

	private val _buffer = ConcurrentLinkedQueue<RecordedPoint>()
	private val _lock = Mutex()

	private val _rangeMin = AtomicInt(0)
	private val _rangeMax = AtomicInt(100)

	private var _recorder: AudioRecord? = null
	private var _minBufferSize: Int = 0

	suspend fun initiateRecorder() {

		if (!_hasRecordPermission) {
			Log.d(TAG, "MISSING PERMISSION")
			return
		}

		if (_recorder != null) {
			Log.d(TAG, "RECORDER ALREADY INITIATED")
			return
		}

		val audioSettings = settings.audioSettings()
		val sampleRate = audioSettings.quality.sampleRate
		val audioFormat = AudioFormat.ENCODING_PCM_16BIT
		val channelConfig = if (audioSettings.enableStereo) AudioFormat.CHANNEL_IN_STEREO
		else AudioFormat.CHANNEL_IN_MONO

		try {
			_minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
			if (_minBufferSize == AudioRecord.ERROR || _minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
				Log.e(TAG, "AudioRecord.getMinBufferSize error: $_minBufferSize")
				return
			}
			_recorder = AudioRecord(
				MediaRecorder.AudioSource.MIC,
				sampleRate,
				channelConfig,
				audioFormat,
				_minBufferSize * 2
			)

			if (_recorder?.state != AudioRecord.STATE_INITIALIZED) {
				Log.e(TAG, "AudioRecord initialization failed for visualizer.")
				releaseRecorder() // Ensure cleanup
				return
			}
		} catch (e: IllegalArgumentException) {
			e.printStackTrace()
		}
	}

	suspend fun startRecorder() {
		if (_recorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
			Log.d(TAG, "RECORDER STATE RECORDING CANNOT START AGAIN")
			return
		}
		if (_recorder == null) {
			Log.d(TAG, "RECORDER WAS NOT INITIATED ")
			initiateRecorder()
		}
		try {
			_recorder?.startRecording()
		} catch (e: IllegalStateException) {
			Log.d(TAG, "WRONG STATE", e)
		}
	}

	fun stopRecorder() {
		if (_recorder?.recordingState == AudioRecord.RECORDSTATE_STOPPED) {
			Log.d(TAG, "RECORDER STATE IS ALREADY STOPPED")
			return
		}
		try {
			_recorder?.stop()
		} catch (e: IllegalStateException) {
			Log.d(TAG, "WRONG STATE", e)
		}
	}

	fun releaseRecorder() {
		try {
			if (_recorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
				Log.d(TAG, "AUDIO RECORDER WAS RECORDING STOPPING IT")
				_recorder?.stop()
			}
			_recorder?.release()
			_recorder = null
			_minBufferSize = 0
		} catch (e: Exception) {
			Log.d(TAG, "FAILED TO RELEASE RECORDER", e)
		} finally {
			clearBuffer()
		}
	}

	fun readAmplitudeBuffered(recorderState: RecorderState): Flow<List<RecordedPoint>> {
		return readRecorderRawBytes(recorderState)
			.flatMapLatest(::toFixedSizeCollection)
			.mapLatest { points ->
				points.smoothen(factor = .25f)
					.normalize(max = _rangeMax.load(), min = _rangeMin.load())
					.padListWithExtra(bufferSize * 2)
					.toProperSequence(bufferSize)
					.distinctBy { it.timeInMillis }
			}.flowOn(Dispatchers.Default)
	}


	private fun readRecorderRawBytes(state: RecorderState): Flow<Float> = flow {
		try {
			if (!state.canReadAmplitudes) {
				clearBuffer()
				emit(0f)
				return@flow
			}

			val invalids = arrayOf(AudioRecord.ERROR_INVALID_OPERATION, AudioRecord.ERROR_BAD_VALUE)
			if (_recorder == null) return@flow

			val pcmBuffer = ShortArray(_minBufferSize / 2)
			var shortsRead: Int

			while (state == RecorderState.RECORDING && currentCoroutineContext().isActive) {
				// ensure the current coroutine is active otherwise
				currentCoroutineContext().ensureActive()

				shortsRead = _recorder?.read(pcmBuffer, 0, pcmBuffer.size) ?: break
				if (shortsRead in invalids) {
					Log.e(TAG, "RECORDER CANNOT READ BYTES!")
					break // Exit loop on critical error
				}

				// these are raw bytes
				val rmsValue = rms(pcmBuffer)
				emit(maxOf(rmsValue, .0f))
				// check if audio source set otherwise amp is zero
				// read the values here
				delay(delayRate)
			}
		} catch (e: CancellationException) {
			throw e
		} catch (err: IllegalStateException) {
			Log.e(TAG, "ILLEGAL STATE", err)
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}.flowOn(Dispatchers.IO)

	/**
	 * Clears the buffer if it contains any value and emit an end zero
	 */
	private fun clearBuffer() {
		if (_buffer.isNotEmpty()) {
			Log.d(TAG, "CLEARING VALUES")
			_buffer.clear()
			_rangeMin.store(0)
			_rangeMax.store(100)
		}
	}


	private fun toFixedSizeCollection(newValue: Float): Flow<List<RecordedPoint>> {
		return flow {
			try {
				updateItemsInList(newValue)
				val max = _rangeMax.load()
				val min = _rangeMin.load()
				// change the max value
				if (newValue > max) {
					Log.d(TAG, "NEW MAX VALUE SET $newValue")
					_rangeMax.store(newValue.toInt())
				}
				// change the min value
				if (newValue < min) {
					Log.d(TAG, "NEW MIN VALUE SET $newValue")
					_rangeMin.store(newValue.toInt())
				}
				val distinctBuffer = _buffer.distinctBy { it.timeInMillis }.toList()
				emit(distinctBuffer)
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}.flowOn(Dispatchers.Default)
	}

	private suspend fun updateItemsInList(newValue: Float) {

		if (_lock.holdsLock(this)) {
			Log.d(TAG, "ITS LOCKED REMOVING ITEMS IS ALREADY PROCESSING")
			return
		}
		_lock.lock(this)

		try {
			val stopWatchTime = stopWatch.elapsedTime.value.toMillisecondOfDay().toLong()
			val entry = (stopWatchTime / bufferSize) * bufferSize
			val point = RecordedPoint(entry, newValue)
			// adds the element to the end of queue
			_buffer.add(point)
			if (_buffer.size >= bufferSize * 3) {
				// remove the first pair
				Log.d(TAG, "REMOVING SOME ITEMS FROM FRONT")
				// removes the elements
				val itemsToRemove = _buffer.take(bufferSize)
				_buffer.removeAll(itemsToRemove)
				// items removed
				Log.d(TAG, "ITEMS REMOVED")
			}
		} catch (e: Exception) {
			e.printStackTrace()
		} finally {
			_lock.unlock()
		}
	}
}