package com.eva.recorder.data.reader

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import com.eva.recorder.data.hasAudioRecordPermission
import com.eva.recorder.domain.models.RecordedPoint
import com.eva.recorder.domain.models.RecorderState
import com.eva.transcribe.domain.AudioTranscriptor
import com.eva.utils.RecorderConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
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
internal class AudioRecordAmplitudeReader(
	private val context: Context,
	private val transcriptor: AudioTranscriptor,
	private val delayRate: Duration = RecorderConstants.AMPS_READ_DELAY_RATE,
	private val bufferSize: Int = RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE,
	private val isTranscriptionsEnabled: Boolean = false,
) {

	private val _buffer = ConcurrentLinkedQueue<RecordedPoint>()

	private val _lock = Any()
	private val _mutex = Mutex()

	private val _rangeMin = AtomicInt(0)
	private val _rangeMax = AtomicInt(100)

	private var _recorder: AudioRecord? = null

	@Volatile
	private var _pcmBufferSize: Int = 0

	// recorder error codes
	private val errorCodes = arrayOf(
		AudioRecord.ERROR_INVALID_OPERATION,
		AudioRecord.ERROR_BAD_VALUE,
		AudioRecord.ERROR
	)

	suspend fun initiateRecorder() {
		if (!context.hasAudioRecordPermission) {
			Log.d(TAG, "MISSING PERMISSION")
			return
		}

		if (_recorder != null) {
			Log.d(TAG, "RECORDER ALREADY INITIATED")
			return
		}
		// don't change the sample rate
		val sampleRate = 16_000
		// encoding is based to 16 bits
		val audioFormat = AudioFormat.ENCODING_PCM_16BIT
		val channelConfig = AudioFormat.CHANNEL_IN_MONO
		val channelCount = 1
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
				MediaRecorder.AudioSource.VOICE_RECOGNITION,
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
			// setup recognizer if transcriptions is enabled
			if (isTranscriptionsEnabled) {
				transcriptor.setUp(sampleRate = sampleRate.toFloat())
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
			transcriptor.cleanUp()
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
			// clean the transcriptor
			transcriptor.cleanUp()
			// release the audio record obj
			_recorder?.release()
			_recorder = null
			_pcmBufferSize = 0
		} catch (e: Exception) {
			Log.d(TAG, "FAILED TO RELEASE RECORDER", e)
		} finally {
			clearBuffer()
		}
	}

	val transcriptionResult: Flow<String>
		get() = transcriptor.recognizedText

	fun readAmplitudeBuffered(recorderState: RecorderState, stopWatchTime: Long) =
		readRecorderRawBytes(recorderState)
			.flatMapLatest { rms -> toFixedSizeCollection(rms, stopWatchTime) }
			.mapLatest { points ->
				points.asSequence()
					.smoothen(factor = .3f)
					.normalize(max = _rangeMax.load(), min = _rangeMin.load())
					.padListWithExtra(bufferSize * 2)
					.toProperSequence(bufferSize)
					.distinctBy { it.timeInMillis }
					.toList()
			}.flowOn(Dispatchers.Default)


	private fun readRecorderRawBytes(state: RecorderState): Flow<Float> = channelFlow {
		// reset the state based on the state
		if (!state.canReadAmplitudes) {
			clearBuffer()
			send(0f)
			return@channelFlow
		}
		if (_recorder == null) return@channelFlow
		try {
			val pcmBuffer = ShortArray(_pcmBufferSize)
			var shortsRead: Int
			var lastVisualizerEmit = 0L

			while (state == RecorderState.RECORDING && currentCoroutineContext().isActive) {
				// ensure the current coroutine is active otherwise
				shortsRead = _recorder?.read(pcmBuffer, 0, pcmBuffer.size) ?: break
				if (shortsRead in errorCodes || shortsRead == 0) break

				// these are raw bytes
				val rmsValue = pcmBuffer.rms(shortsRead)
				val runRecognizer = rmsValue > AudioTranscriptor.MIN_RMS_TO_RECOGNIZE &&
						isTranscriptionsEnabled
				if (runRecognizer && currentCoroutineContext().isActive) {
					// feed this to transcriber
					transcriptor.recognizeAudio(pcmBuffer, shortsRead)
				}
				// cant use delay it will break the recognizer so using
				// throttling
				val now = SystemClock.elapsedRealtime()
				if (now - lastVisualizerEmit >= delayRate.inWholeMilliseconds) {
					trySend(rmsValue)
					lastVisualizerEmit = now
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

	private fun toFixedSizeCollection(newValue: Float, stopWatchTime: Long) = flow {
		try {
			updateItemsInList(newValue, stopWatchTime)
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


	private suspend fun updateItemsInList(newValue: Float, stopWatchTime: Long) {
		_mutex.withLock(_lock) {
			try {
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

	private fun ShortArray.rms(readSize: Int): Float {
		val squaredAvg = take(readSize).map { it * it }.average().toFloat()
		return sqrt(squaredAvg)
	}
}