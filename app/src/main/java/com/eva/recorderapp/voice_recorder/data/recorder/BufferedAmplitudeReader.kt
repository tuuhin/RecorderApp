package com.eva.recorderapp.voice_recorder.data.recorder

import android.media.MediaRecorder
import android.util.Log
import com.eva.recorderapp.voice_recorder.domain.emums.RecorderState
import com.eva.recorderapp.voice_recorder.domain.recorder.VoiceRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val LOGGER_TAG = "AMPLITUDE_READER"

class BufferedAmplitudeReader(
	private val recorder: MediaRecorder?,
	val samplingRate: Duration = 80.milliseconds
) {
	// max amplitude will be 32_768
	private val MAX_AMPLITUDE = 2f.pow(15)

	private val _buffer = ConcurrentLinkedQueue<Int>()

	@Volatile
	private var ampsRange = AmpsRange()

	private val _isAudioSourceNull: Boolean
		get() = recorder?.activeRecordingConfiguration?.audioSource == null

	@OptIn(ExperimentalCoroutinesApi::class)
	fun readAmplitudeBuffered(state: RecorderState): Flow<FloatArray> {
		val sampledAmps = when (state) {
			RecorderState.COMPLETED, RecorderState.CANCELLED -> clearBufferAndEmitZero()
			else -> readSampleAmplitude(state)
		}
		return sampledAmps.flatMapLatest(::flowToFixedSizeCollection)
			.mapLatest(::smoothen)
			.mapLatest { it.normalize() }
			.flowOn(Dispatchers.Default)
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
				emit(amplitude)
				delay(samplingRate)
			}
		} catch (e: CancellationException) {
			// if the child flow is canceled while suspending in delay method
			// throw cancelation exception
			throw e
		} catch (e: IllegalStateException) {
			Log.e(LOGGER_TAG, "ILLEGAL STATE")
		} catch (e: Exception) {
			e.printStackTrace()
		}

	}.flowOn(Dispatchers.IO)

	/**
	 * Clears the buffer if it contains any value and emit a end zero
	 */
	private fun clearBufferAndEmitZero() = flow<Int> {
		if (_buffer.isNotEmpty()) {
			_buffer.clear()
			ampsRange = AmpsRange()
		}
		emit(0)
	}

	/**
	 * Adds the new value to [_buffer] and compute a flow out of it
	 * The flow will have a fix size [maxBufferSize].
	 */
	private fun flowToFixedSizeCollection(newValue: Int): Flow<List<Int>> {
		return flow {
			try {
				if (_buffer.size >= VoiceRecorder.RECORDER_AMPLITUDES_BUFFER_SIZE)
					_buffer.poll()

				_buffer.offer(newValue)

				val maxValue = _buffer.maxOrNull() ?: 0
				val minValue = _buffer.minOrNull() ?: 0
				if (maxValue > ampsRange.max) {
					Log.d(LOGGER_TAG, "NEW MAX VALUE SET $maxValue")
					ampsRange = ampsRange.copy(max = maxValue)
				}
				if (minValue < ampsRange.min) {
					Log.d(LOGGER_TAG, "NEW MIN VALUE SET $minValue")
					ampsRange = ampsRange.copy(min = minValue)
				}
				emit(_buffer.toList())
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}.flowOn(Dispatchers.Default)
	}

	private fun List<Float>.normalize(): FloatArray {

		val range = ampsRange.range
		// if range is zero  return empty
		// that's probably the empty case
		if (range <= 0) return floatArrayOf()

		val normalizedValue = map { amp ->
			val normalizedValue = (amp - ampsRange.min).toFloat() / range
			normalizedValue.coerceIn(0f..1f)
		}
		return normalizedValue.toFloatArray()
	}

	private fun smoothen(
		data: List<Int>,
		factor: Float = 0.3f
	): List<Float> {
		var prev = 0f
		val out = buildList<Float> {
			data.forEach { amplitude ->
				val smooth = (prev * factor + amplitude * (1 - factor))
				add(smooth)
				prev = smooth
			}
		}
		return out
	}

	private data class AmpsRange(
		val max: Int = 0,
		val min: Int = 0
	) {
		val range = max - min
	}

}