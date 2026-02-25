package com.eva.recorder.data.recorder

import android.util.Log
import com.eva.recorder.domain.models.RecordedPoint
import com.eva.recorder.domain.recorder.AudioByteDataProvider
import com.eva.recorder.domain.recorder.AudioVisualDataProvider
import com.eva.recorder.domain.stopwatch.RecorderStopWatch
import com.eva.utils.RecorderConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration

private const val TAG = "AUDIO_VISUALIZER"

@OptIn(
	ExperimentalAtomicApi::class,
	ExperimentalCoroutinesApi::class,
	FlowPreview::class
)
internal class AudioVisualizerDataProviderImpl(
	source: AudioByteDataProvider,
	private val stopWatch: RecorderStopWatch,
	delayRate: Duration = RecorderConstants.AMPS_READ_DELAY_RATE,
	private val bufferSize: Int = RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE,
) : AudioVisualDataProvider {

	private val _buffer = ConcurrentLinkedQueue<RecordedPoint>()

	private val _lock = Any()
	private val _mutex = Mutex()

	private val _rangeMin = AtomicInt(0)
	private val _rangeMax = AtomicInt(100)

	private val slowedRmsPoints: Flow<Float> = source.stream
		.buffer(Channel.CONFLATED)
		.onStart {
			clearBuffer()
			// initial emission required to start the flow
			// otherwise combine will not work
			emit(shortArrayOf() to -1)
		}
		.transform { (shortArray, size) ->
			if (shortArray.isEmpty() || size < 0) clearBuffer()
			// emit is required to be sent which will clear the thing
			emit(shortArray to size)
		}
		.map { (shorts, size) -> if (size >= 0) shorts.rms(size) else .0f }
		.flowOn(Dispatchers.Default)
		.sample(delayRate)
		.onCompletion {
			Log.d(TAG, "FLOW READ IS COMPLETED")
			clearBuffer()
		}

	override val dataPoints: Flow<List<RecordedPoint>>
		get() = combine(slowedRmsPoints, stopWatch.elapsedTime) { rms, t -> rms to t.toMillisecondOfDay() }
			.flatMapLatest { (rms, time) -> toFixedSizeCollection(rms, time) }
			.mapLatest { points -> points.normalizedAndPadded() }
			.flowOn(Dispatchers.Default)


	private fun toFixedSizeCollection(newValue: Float, stopWatchTime: Int) = flow {
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


	private suspend fun updateItemsInList(newValue: Float, stopWatchTime: Int) {
		_mutex.withLock(_lock) {
			try {
				val entry = (stopWatchTime / bufferSize) * bufferSize
				val point = RecordedPoint(entry.toLong(), newValue)
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

	private fun clearBuffer() {
		if (_buffer.isEmpty()) return
		Log.d(TAG, "CLEARING VALUES")
		_buffer.clear()
		_rangeMin.store(0)
		_rangeMax.store(100)
	}

	private fun List<RecordedPoint>.normalizedAndPadded() = asSequence()
		.smoothen(factor = .3f)
		.normalize(max = _rangeMax.load(), min = _rangeMin.load())
		.padListWithExtra(bufferSize * 2)
		.toProperSequence(bufferSize)
		.distinctBy { it.timeInMillis }
		.toList()

}