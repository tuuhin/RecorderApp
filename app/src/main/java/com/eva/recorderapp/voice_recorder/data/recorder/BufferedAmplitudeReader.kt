package com.eva.recorderapp.voice_recorder.data.recorder

import android.media.MediaRecorder
import android.util.Log
import com.eva.recorderapp.voice_recorder.domain.emums.RecorderState
import com.eva.recorderapp.voice_recorder.domain.util.toNormalizedValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val LOGGER_TAG = "AMPLITUDE_READER"

class BufferedAmplitudeReader(
	private val recorder: MediaRecorder?,
	val maxBufferSize: Int = 80,
	val samplingInterval: Duration = 80.milliseconds
) {
	private val _buffer = ConcurrentLinkedQueue<Int>()

	private val _isAudioSourceNull: Boolean
		get() = recorder?.activeRecordingConfiguration?.audioSource == null

	@OptIn(ExperimentalCoroutinesApi::class)
	fun readAmplitudeBuffered(state: RecorderState): Flow<FloatArray> {
		val sampledAmps = when (state) {
			RecorderState.COMPLETED, RecorderState.CANCELLED -> clearBufferAndEmitZero()
			else -> readSampleAmplitude(state)
		}
		return sampledAmps
			.flatMapLatest(::flowToFixedSizeCollection)
			.toNormalizedValues()
	}

	private fun readSampleAmplitude(state: RecorderState): Flow<Int> = flow {
		try {
			while (recorder != null && state == RecorderState.RECORDING) {
				// check if audio source set
				if (_isAudioSourceNull) {
					Log.i(LOGGER_TAG, "AUDIO SOURCE NOT CONFIGURED")
					break
				}
				delay(samplingInterval)
				// record the max amplitude of the sample
				val amplitude = recorder.maxAmplitude
				emit(amplitude)
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
		if (_buffer.isNotEmpty())
			_buffer.clear()
		emit(0)
	}

	/**
	 * Adds the new value to [_buffer] and compute a flow out of it
	 * The flow will have a fix size [maxBufferSize].
	 */
	private fun flowToFixedSizeCollection(newValue: Int): Flow<List<Int>> {
		return flow {
			try {
				if (_buffer.size >= maxBufferSize)
					_buffer.poll()

				_buffer.offer(newValue)

				val paddedList = if (maxBufferSize - _buffer.size > 0) {
					val extras = List(maxBufferSize - _buffer.size) { 0 }
					_buffer.plus(extras)
				} else _buffer

				emit(paddedList.toList())
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}.flowOn(Dispatchers.Default)
	}

}