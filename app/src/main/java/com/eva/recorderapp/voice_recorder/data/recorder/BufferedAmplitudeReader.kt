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
import kotlin.time.Duration.Companion.milliseconds

private const val LOGGER_TAG = "AMPLITUDE_READER"

class BufferedAmplitudeReader(
	private val recorder: MediaRecorder?,
	val maxBufferSize: Int = 100
) {
	private val _buffer = ConcurrentLinkedQueue<Int>()

	private val _isAudioSourceConfigured: Boolean?
		get() = recorder?.activeRecordingConfiguration?.audioSource != null

	@OptIn(ExperimentalCoroutinesApi::class)
	fun readAmplitudeBuffered(state: RecorderState): Flow<FloatArray> {
		return readSampleAmplitude(state)
			.flatMapLatest(::flowToFixedSizeCollection)
			.toNormalizedValues()
			.flowOn(Dispatchers.Default)
	}

	private fun readSampleAmplitude(state: RecorderState): Flow<Int> = flow {
		try {
			while (state == RecorderState.RECORDING) {
				// check if audio source set
				if (_isAudioSourceConfigured == false) {
					Log.i(LOGGER_TAG, "AUDIO SOURCE NOT CONFIGURED")
					break
				}
				delay(50.milliseconds)
				// record the max amplitude of the sample
				val amplitude = recorder?.maxAmplitude ?: 0
				emit(amplitude)
			}
			if (state == RecorderState.COMPLETED) {
				// clearing the buffer ensures that again we
				// read values from start of the buffer
				Log.d(LOGGER_TAG, "CLEARING BUFFER AS RECORDING COMPLETED")
				_buffer.clear()
				// hope this fixes the glitz
				emit(0)
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