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
import com.eva.recorder.domain.models.RecordedPoint
import com.eva.recorder.domain.models.RecorderState
import com.eva.recorder.domain.stopwatch.RecorderStopWatch
import com.eva.utils.RecorderConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.sqrt
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
	private val delayRate: Duration = RecorderConstants.AMPS_READ_DELAY_RATE,
	private val bufferSize: Int = RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE,
) {

	private val _hasRecordPermission: Boolean
		get() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
				PermissionChecker.PERMISSION_GRANTED

	private val _buffer = ConcurrentLinkedQueue<RecordedPoint>()

	private val _lock = Any()
	private val _mutex = Mutex()

	private val _rangeMin = AtomicInt(0)
	private val _rangeMax = AtomicInt(100)

	private var _recorder: AudioRecord? = null

	@Volatile
	private var _pcmBufferSize: Int = 0

	fun initiateRecorder(sampleRate: Int, isStereo: Boolean) {
		if (!_hasRecordPermission) {
			Log.d(TAG, "MISSING PERMISSION")
			return
		}

		if (_recorder != null) {
			Log.d(TAG, "RECORDER ALREADY INITIATED")
			return
		}
		// encoding is based to 16 bits
		val audioFormat = AudioFormat.ENCODING_PCM_16BIT
		val channelConfig = if (isStereo) AudioFormat.CHANNEL_IN_STEREO
		else AudioFormat.CHANNEL_IN_MONO

		val channelCount = if (isStereo) 2 else 1
		val bytesPerSample = 2

		try {
			val bufferSize = AudioRecord
				.getMinBufferSize(sampleRate, channelConfig, audioFormat)
			if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
				Log.e(TAG, "AudioRecord.getMinBufferSize error: $_pcmBufferSize")
				return
			}

			_pcmBufferSize = bufferSize / (bytesPerSample * channelCount)
			Log.d(TAG, "PCM BUFFER SIZE :$_pcmBufferSize")

			_recorder = AudioRecord(
				MediaRecorder.AudioSource.MIC,
				sampleRate,
				channelConfig,
				audioFormat,
				bufferSize * 2
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

	fun startRecorder() {
		if (_recorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
			Log.d(TAG, "RECORDER STATE RECORDING CANNOT START AGAIN")
			return
		}
		if (_recorder == null) {
			Log.d(TAG, "RECORDER WAS NOT INITIATED ")
			throw Exception("Audio Record instance not initiated")
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
			_pcmBufferSize = 0
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
				points.asSequence()
					.smoothen(factor = .3f)
					.normalize(max = _rangeMax.load(), min = _rangeMin.load())
					.padListWithExtra(bufferSize * 2)
					.toProperSequence(bufferSize)
					.distinctBy { it.timeInMillis }
					.toList()
			}.flowOn(Dispatchers.Default)
	}


	private fun readRecorderRawBytes(state: RecorderState): Flow<Float> = flow {
		try {
			if (!state.canReadAmplitudes) {
				clearBuffer()
				emit(0f)
				return@flow
			}

			val invalids = arrayOf(
				AudioRecord.ERROR_INVALID_OPERATION,
				AudioRecord.ERROR_BAD_VALUE,
				AudioRecord.ERROR
			)
			if (_recorder == null) return@flow

			val pcmBuffer = ShortArray(_pcmBufferSize)
			var shortsRead: Int

			while (state == RecorderState.RECORDING && currentCoroutineContext().isActive) {
				// ensure the current coroutine is active otherwise
				shortsRead = _recorder?.read(pcmBuffer, 0, pcmBuffer.size) ?: break
				if (shortsRead in invalids) break
				if (shortsRead == 0) break

				if (currentCoroutineContext().isActive) {
					// these are raw bytes
					val rmsValue = pcmBuffer.rms(shortsRead)
					emit(rmsValue)
					// check if audio source set otherwise amp is zero
					// read the values here
					delay(delayRate)
				}
			}
		} catch (e: Exception) {
			if (e is CancellationException) Log.d(TAG, "NO MORE PROCESSING VALUES")
			e.printStackTrace()
		}
	}.flowOn(Dispatchers.IO)

	/**
	 * Clears the buffer if it contains any value and emit an end zero
	 */
	private fun clearBuffer() {
		if (_buffer.isEmpty()) return
		Log.d(TAG, "CLEARING VALUES")
		_buffer.clear()
		_rangeMin.store(0)
		_rangeMax.store(100)
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
				val distinctBuffer = _buffer.distinctBy { it.timeInMillis }
				emit(distinctBuffer)
			} catch (e: Exception) {
				if (e is CancellationException) Log.d(TAG, "UPDATE BUFFER CANCELLED")
				e.printStackTrace()
			}
		}.flowOn(Dispatchers.Default)
	}

	private suspend fun updateItemsInList(newValue: Float) {
		_mutex.withLock(_lock) {
			try {
				val stopWatchTime = stopWatch.elapsedTime.value.toMillisecondOfDay().toLong()
				val entry = (stopWatchTime / bufferSize) * bufferSize
				val point = RecordedPoint(entry, newValue)
				// adds the element to the end of queue
				_buffer.offer(point)
				if (_buffer.size >= bufferSize * 3) {
					// remove the first pair
					Log.d(TAG, "REMOVING SOME ITEMS FROM FRONT")
					// removes the elements via polling them out
					repeat(bufferSize) {
						// if polling failed return from the block
						_buffer.poll() ?: return@repeat
					}
					// items removed
					Log.d(TAG, "ITEMS REMOVED")
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}

	internal suspend fun ShortArray.rms(readSize: Int): Float {
		// lets not context switch
		return coroutineScope {
			val squaredAvg = take(readSize).map { it * it }.average().toFloat()
			sqrt(squaredAvg)
		}
	}
}