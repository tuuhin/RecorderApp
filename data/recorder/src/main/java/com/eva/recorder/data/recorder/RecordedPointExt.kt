package com.eva.recorder.data.recorder

import com.eva.recorder.domain.models.RecordedPoint
import kotlin.math.sqrt


internal fun List<RecordedPoint>.normalize(min: Int = 0, max: Int = 1): List<RecordedPoint> {
    if (isEmpty()) return this

    val diff = (max - min).let { if (it <= 0) 1f else it.toFloat() }

    return buildList(size) {
        for (i in this@normalize.indices) {
            val point = this@normalize[i]
            val value = if (point.isPaddingPoint) 0f
            else ((point.rmsValue - min) / diff).coerceIn(0f, 1f)
            add(point.copy(rmsValue = value))
        }
    }
}

internal fun List<RecordedPoint>.smoothen(smoothness: Float = .7f): List<RecordedPoint> {
    if (size < 2 || smoothness == 0f) return this

    val source = this
    val current = 1f - smoothness

    return buildList(source.size) {
        add(source[0])

        for (i in 1 until source.size) {
            val point = source[i]
            if (point.isPaddingPoint) {
                add(point)
                continue
            }
            val smoothed = source[i - 1].rmsValue * smoothness + point.rmsValue * current
            add(point.copy(rmsValue = smoothed))
        }
    }
}

internal fun List<RecordedPoint>.padListWithExtra(
    targetSize: Int,
    blockSize: Int,
    extra: Int = 10,
): List<RecordedPoint> {
    val source = this
    val missing = targetSize - source.size
    val amount = if (missing > 0) missing + extra else extra
    val lastTime = if (source.isEmpty()) 0L else source.last().timeInMillis
    val firstPad = if (source.isEmpty()) 0 else 1

    return buildList(source.size + amount) {
        addAll(source)
        for (i in firstPad until amount) {
            add(RecordedPoint(lastTime + i * blockSize, 0f, true))
        }
    }
}

internal fun List<RecordedPoint>.toProperSequence(eachBlockSize: Int): List<RecordedPoint> {
    val source = this
    if (source.isEmpty()) return emptyList()

    return buildList(source.size * 2) {
        // Output is time-ascending, so comparing with the last element is equivalent to distinctBy
        fun addDistinct(point: RecordedPoint) {
            if (isEmpty() || last().timeInMillis != point.timeInMillis) add(point)
        }

        var expected = source[0]
        var start = expected.timeInMillis

        for (i in 1 until source.size) {
            val actual = source[i]
            while (start < actual.timeInMillis) {
                addDistinct(expected.copy(timeInMillis = start))
                start += eachBlockSize
            }
            addDistinct(actual)
            expected = actual
            start += eachBlockSize
        }
    }
}

fun ShortArray.rms(readSize: Int): Float {
    val n = minOf(readSize, size)
    if (n <= 0) return 0f
    var sum = 0L
    for (i in 0 until n) {
        val s = this[i].toInt()
        sum += s * s
    }
    return sqrt(sum.toDouble() / n).toFloat()
}
