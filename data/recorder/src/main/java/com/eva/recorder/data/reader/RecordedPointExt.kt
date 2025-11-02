package com.eva.recorder.data.reader

import com.eva.recorder.domain.models.RecordedPoint
import kotlin.math.abs

internal fun Sequence<RecordedPoint>.normalize(max: Int, min: Int): Sequence<RecordedPoint> {
	val range = (max - min).let { diff -> if (diff <= 0) 1 else diff }
	return map { point ->
		point.copy(
			rmsValue = (abs(point.rmsValue - min) / range)
				.coerceIn(0f..1f)
		)
	}
}

internal fun Sequence<RecordedPoint>.smoothen(factor: Float = 0.3f): Sequence<RecordedPoint> {
	var prev = 0f
	return map { point ->
		prev = lerp(prev, point.rmsValue, factor)
		point.copy(rmsValue = prev)
	}
}

internal fun Sequence<RecordedPoint>.padListWithExtra(
	bufferSize: Int,
	extra: Int = 10
): Sequence<RecordedPoint> = sequence {

	val seen = mutableSetOf<Long>()
	var size = 0
	var lastTime = 0L

	// Yield all incoming elements first, tracking size and last value
	for (point in this@padListWithExtra.iterator()) {
		if (seen.add(point.timeInMillis)) {
			yield(point)
		}
		size++
		lastTime = point.timeInMillis
	}

	// Compute how many extras we need *after* finishing
	val differences = bufferSize - size
	val amount = if (differences > 0) differences + extra else extra

	// Yield the padded extra points lazily
	for (i in 0 until amount) {
		val timeInMillis = lastTime + (i * bufferSize)
		if (seen.add(timeInMillis)) {
			yield(RecordedPoint(timeInMillis, 0f, true))
		}
	}
}

internal fun Sequence<RecordedPoint>.toProperSequence(eachBlockSize: Int): Sequence<RecordedPoint> {
	return sequence {

		val iterator = this@toProperSequence.iterator()
		if (!iterator.hasNext()) return@sequence

		var expected = iterator.next()
		var start = expected.timeInMillis

		for (actualPoint in iterator) {
			while (start < actualPoint.timeInMillis) {
				yield(expected.copy(timeInMillis = start))
				start += eachBlockSize
			}
			yield(actualPoint)
			expected = actualPoint
			start += eachBlockSize
		}
	}
}

private fun lerp(v0: Float, v1: Float, t: Float): Float {
	return (1 - t) * v1 + t * v0
}