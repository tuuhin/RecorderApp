package com.eva.recorder.data.recorder

import com.eva.recorder.domain.models.RecordedPoint
import com.eva.recorder.domain.recorder.AudioByteDataProvider
import com.eva.recorder.domain.recorder.AudioVisualDataProvider
import com.eva.recorder.domain.stopwatch.RecorderStopWatch
import com.eva.utils.RecorderConstants
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.sample

@OptIn(FlowPreview::class, ExperimentalAtomicApi::class)
internal class AudioVisualizerDataProviderImpl(
    source: AudioByteDataProvider,
    stopWatch: RecorderStopWatch,
    delayRate: Duration = RecorderConstants.AMPS_READ_DELAY_RATE,
    private val bufferSize: Int = RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE,
) : AudioVisualDataProvider {

    // buffer operation done with a lock
    private val _lock = Any()
    private val _buffer = ArrayDeque<RecordedPoint>()

    // atomic values for ranges
    private val _rMin = AtomicInt(0)
    private val _rMax = AtomicInt(0)

    private val slowedRmsPoints: Flow<Float> = source.stream
        .buffer(Channel.CONFLATED)
        .onStart {
            clearBuffer()
            // initial emission required so combine can start
            emit(shortArrayOf() to -1)
        }
        .onEach { (shorts, size) -> if (shorts.isEmpty() || size < 0) clearBuffer() }
        .map { (shorts, size) -> if (size >= 0) shorts.rms(size) else 0f }
        .flowOn(Dispatchers.Default)
        .sample(delayRate)

    override val dataPoints: Flow<List<RecordedPoint>> =
        combine(slowedRmsPoints, stopWatch.elapsedTime) { rms, t -> rms to t.toMillisecondOfDay() }
            .map { (rms, time) -> appendAndRender(rms, time) }
            .flowOn(Dispatchers.Default)
            .onCompletion { clearBuffer() }


    private fun appendAndRender(newValue: Float, stopWatchTime: Int): List<RecordedPoint> {
        val snapshot = synchronized(_lock) {

            // don't consider any values changes if stop watch time is 0
            // which means stopwatch is being idle and not started
            if (stopWatchTime == 0) return@synchronized emptyList()

            // update the ranges
            updateRange(newValue)

            val entry = ((stopWatchTime / bufferSize) * bufferSize).toLong()

            // check the entry time-millis
            if (_buffer.lastOrNull()?.timeInMillis != entry) {
                _buffer.addLast(RecordedPoint(entry, newValue))

                // clear the values if it's too long
                if (_buffer.size >= bufferSize * 3) {
                    repeat(bufferSize) { _buffer.removeFirst() }
                }
            }

            // smoothen tha values and normalize it
            _buffer.smoothen(.3f)
                .normalize(min = _rMin.load(), max = _rMax.load())
        }

        return snapshot
            .toProperSequence(bufferSize)
            .padListWithExtra(targetSize = bufferSize * 2, blockSize = bufferSize)
    }

    private fun updateRange(newValue: Float) {
        if (newValue > _rMax.load()) _rMax.store(newValue.toInt())
        if (newValue < _rMin.load()) _rMin.store(newValue.toInt())
    }

    private fun clearBuffer() {
        // buffer operation need to do with a lock
        synchronized(_lock) {
            if (_buffer.isEmpty()) return@synchronized
            _buffer.clear()
        }
        _rMin.store(0)
        _rMax.store(100)
    }
}
