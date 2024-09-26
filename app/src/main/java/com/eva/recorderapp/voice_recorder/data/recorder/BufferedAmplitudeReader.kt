package com.eva.recorderapp.voice_recorder.data.recorder

import android.media.MediaRecorder
import android.util.Log
import com.eva.recorderapp.voice_recorder.domain.recorder.MicrophoneDataPoint
import com.eva.recorderapp.voice_recorder.domain.recorder.VoiceRecorder
import com.eva.recorderapp.voice_recorder.domain.recorder.emums.RecorderState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val LOGGER_TAG = "AMPLITUDE_READER"

class BufferedAmplitudeReader(
	private val recorder: MediaRecorder?,
	private val delayRate: Duration = 80.milliseconds,
	private val bufferSize: Int = VoiceRecorder.RECORDER_AMPLITUDES_BUFFER_SIZE,
) {
	private val _buffer = ConcurrentLinkedQueue<Pair<Long, Int>>()
	private val _ampIdx = AtomicLong(0)

	private val ampsRange = MutableStateFlow(AmpsRange())

	private val _isAudioSourceNull: Boolean
		get() = recorder?.activeRecordingConfiguration?.audioSource == null

	@OptIn(ExperimentalCoroutinesApi::class)
	fun readAmplitudeBuffered(state: RecorderState): Flow<List<MicrophoneDataPoint>> {
		return when {
			// show the amplitudes when its recording or paused
			state.canReadAmplitudes -> readSampleAmplitude(state)
				.flatMapLatest(::flowToFixedSizeCollection)
				.mapLatest(::smoothen)
				.map { it.normalize() }
				.flowOn(Dispatchers.Default)

			else -> clearBufferAndEmptyFlow()
		}
	}

	private fun readSampleAmplitude(state: RecorderState): Flow<Int> = flow {
		try {
			while (recorder != null && state == RecorderState.RECORDING) {
				// check if audio source set
				if (_isAudioSourceNull) {
					Log.i(LOGGER_TAG, "AUDIO SOURCE NOT CONFIGURED")
					break
				}
				// record the max amplitude of the sample
				// multiply with 0.707 to get the rms value
				val amplitude = recorder.maxAmplitude
				// ensure the current coroutine is active
				if (!currentCoroutineContext().isActive) break
				// emit and delay
				emit(amplitude)
				delay(delayRate)
			}
		} catch (e: CancellationException) {
			// if the child flow is canceled while suspending in delay method
			// throw cancellation exception
			throw e
		} catch (e: IllegalStateException) {
			Log.e(LOGGER_TAG, "ILLEGAL STATE")
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}.flowOn(Dispatchers.IO)


	/**
	 * Clears the buffer if it contains any value and emit an end zero
	 */
	private fun clearBufferAndEmptyFlow() = flow<List<MicrophoneDataPoint>> {
		if (_buffer.isNotEmpty()) {
			Log.d(LOGGER_TAG, "RE_INIT THE VALUES")
			_buffer.clear()
			ampsRange.update { AmpsRange() }
			_ampIdx.set(0)
		}
		emit(emptyList())
	}

	/**
	 * Adds the new value to [_buffer] and compute a flow out of it
	 */
	private fun flowToFixedSizeCollection(newValue: Int): Flow<List<Pair<Long, Int>>> =
		flow {
			try {
				// remove the items from the queue if the size exceeds
				if (_buffer.size >= bufferSize * 3) {
					// remove the first pair
					Log.d(LOGGER_TAG, "REMOVING SOME ITEMS FROM FRONT")
					repeat(bufferSize) {
						_buffer.poll()
					}
					Log.d(LOGGER_TAG, "REMOVING ITEMS DONE")
				}
				// add the new one
				_buffer.offer(_ampIdx.getAndIncrement() to newValue)

				// change the max value
				if (newValue > ampsRange.value.max) {
					Log.d(LOGGER_TAG, "NEW MAX VALUE SET $newValue")
					ampsRange.update { range -> range.copy(max = newValue) }
				}
				// change the min value
				if (newValue < ampsRange.value.min) {
					Log.d(LOGGER_TAG, "NEW MIN VALUE SET $newValue")
					ampsRange.update { range -> range.copy(min = newValue) }
				}
				emit(_buffer.toList())
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}.flowOn(Dispatchers.Default)


	private fun List<MicrophoneDataPoint>.normalize(): List<MicrophoneDataPoint> {
		val ampRangeValue = ampsRange.value
		val range = ampRangeValue.range
		// if range is zero  return empty
		// that's probably the empty case
		if (range <= 0) return emptyList()

		return map { (idx, amp) ->
			val normal = (amp - ampRangeValue.min) / range
			idx to normal.coerceIn(0f..1f)
		}
	}

	private fun smoothen(
		data: List<Pair<Long, Int>>,
		factor: Float = 0.3f,
	): List<MicrophoneDataPoint> {
		var prev = 0f
		return buildList {
			data.forEach { (idx, amplitude) ->
				val smooth = lerp(prev, amplitude.toFloat(), factor)
				add(idx to smooth)
				prev = smooth
			}
		}
	}

	private fun lerp(v0: Float, v1: Float, t: Float): Float {
		return (1 - t) * v1 + t * v0
	}
}

private data class AmpsRange(
	val max: Int = 0,
	val min: Int = 0,
) {
	val range: Int
		get() = max - min
}
