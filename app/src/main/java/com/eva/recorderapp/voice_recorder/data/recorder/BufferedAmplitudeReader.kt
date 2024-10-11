package com.eva.recorderapp.voice_recorder.data.recorder

import android.media.MediaRecorder
import android.util.Log
import com.eva.recorderapp.voice_recorder.domain.recorder.MicrophoneDataPoint
import com.eva.recorderapp.voice_recorder.domain.recorder.RecorderStopWatch
import com.eva.recorderapp.voice_recorder.domain.recorder.emums.RecorderState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val LOGGER_TAG = "AMPLITUDE_READER"
private typealias MicroPhoneDataPointsInt = Pair<Long, Int>

class BufferedAmplitudeReader(
	private val recorder: MediaRecorder?,
	private val stopWatch: RecorderStopWatch,
	private val delayRate: Duration = 80.milliseconds,
	private val bufferSize: Int = 80,
) {
	private val _buffer = ConcurrentLinkedQueue<MicroPhoneDataPointsInt>()
	private val _lock = Mutex()

	private val _ampsRange = MutableStateFlow(AmpsRange())

	private val _isAudioSourceNull: Boolean
		get() = recorder?.activeRecordingConfiguration?.audioSource == null

	@OptIn(ExperimentalCoroutinesApi::class)
	fun readAmplitudeBuffered(state: RecorderState): Flow<List<MicrophoneDataPoint>> {
		return when {
			// show the amplitudes when its recording or paused
			state.canReadAmplitudes -> readSampleAmplitude(state)
				.catch { err -> Log.e(LOGGER_TAG, "ERROR: ${err.message}") }
				.flatMapLatest(::toFixedSizeCollection)
				.mapLatest(::smoothen)
				.map { it.normalize() }
				.flowOn(Dispatchers.Default)

			else -> clearBufferAndEmptyFlow()
		}
	}

	private fun readSampleAmplitude(state: RecorderState): Flow<Int> = flow {
		try {
			while (recorder != null && state == RecorderState.RECORDING) {
				// ensure the current coroutine is active
				if (!currentCoroutineContext().isActive) break
				// check if audio source set otherwise amp is zero
				val amplitude = if (_isAudioSourceNull) {
					Log.i(LOGGER_TAG, "AUDIO SOURCE NOT CONFIGURED")
					0
				} else {
					// record the max amplitude of the sample
					recorder.maxAmplitude
				}
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
	private fun clearBufferAndEmptyFlow(): Flow<List<MicrophoneDataPoint>> {
		if (_buffer.isNotEmpty()) {
			Log.d(LOGGER_TAG, "CLEARING VALUES")
			_buffer.clear()
			_ampsRange.update { AmpsRange() }
		}
		return flow {
			emit(emptyList())
		}
	}

	/**
	 * Adds the new value to [_buffer] and compute a flow out of it
	 */
	private fun toFixedSizeCollection(newValue: Int): Flow<List<MicroPhoneDataPointsInt>> {
		return flow {
			try {
				updateItemsInList(newValue)
				// change the max value
				if (newValue > _ampsRange.value.max) {
					Log.d(LOGGER_TAG, "NEW MAX VALUE SET $newValue")
					_ampsRange.update { range -> range.copy(max = newValue) }
				}
				// change the min value
				if (newValue < _ampsRange.value.min) {
					Log.d(LOGGER_TAG, "NEW MIN VALUE SET $newValue")
					_ampsRange.update { range -> range.copy(min = newValue) }
				}
				val distinctBuffer = _buffer.distinctBy { it.first }.toList()
				emit(distinctBuffer)
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}.flowOn(Dispatchers.Default)
	}

	private suspend fun updateItemsInList(newValue: Int) {
		if (_lock.holdsLock(this)) {
			Log.d(LOGGER_TAG, "ITS LOCKED REMOVING ITEMS IS ALREADY PROCESSING")
			return
		}
		_lock.lock(this)
		try {
			val stopWatchTime = stopWatch.elapsedTime.value.toMillisecondOfDay().toLong()
			val entry = (stopWatchTime / bufferSize) * bufferSize
			// adds the element to the end of queue
			_buffer.add(entry to newValue)
			if (_buffer.size >= bufferSize * 3) {
				// remove the first pair
				Log.d(LOGGER_TAG, "REMOVING SOME ITEMS FROM FRONT")
				// removes the elements
				val itemsToRemove = _buffer.take(bufferSize).toSet()
				_buffer.removeAll(itemsToRemove)
				// items removed
				Log.d(LOGGER_TAG, "ITEMS REMOVED")
			}
		} catch (e: Exception) {
			e.printStackTrace()
		} finally {
			_lock.unlock()
		}
	}

	private fun List<MicrophoneDataPoint>.normalize(): List<MicrophoneDataPoint> {
		val ampRangeValue = _ampsRange.value
		val range = ampRangeValue.range
		// if range is zero  return empty
		// that's probably the empty case
		if (range <= 0) return emptyList()

		return map { (idx, amp) ->
			val normal = (amp - ampRangeValue.min) / range
			idx to normal.coerceIn(0f..1f)
		}
	}

	private fun smoothen(data: List<MicroPhoneDataPointsInt>, factor: Float = 0.3f)
			: List<MicrophoneDataPoint> {
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
		get() = (max - min).let { range -> if (range <= 0) 1 else range }
}
