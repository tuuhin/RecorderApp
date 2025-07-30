package com.eva.recorder.data.ext

import com.eva.recorder.domain.models.RecordedPoint
import kotlin.math.abs

internal fun List<RecordedPoint>.normalize(max: Int, min: Int): List<RecordedPoint> {
	val range = (max - min).let { if (it <= 0) 1 else it }
	return map { point ->
		point.copy(
			rmsValue = (abs(point.rmsValue - min) / range)
				.coerceIn(0f..1f)
		)
	}
}

internal fun List<RecordedPoint>.smoothen(factor: Float = 0.3f): List<RecordedPoint> {
	var prev = 0f
	return map { point ->
		val smooth = lerp(prev, point.rmsValue, factor)
		prev = smooth
		point.copy(rmsValue = smooth)
	}
}

internal fun List<RecordedPoint>.padListWithExtra(
	bufferSize: Int,
	extra: Int = 10
): List<RecordedPoint> {
	val differences = bufferSize - size
	val lastValue = lastOrNull()?.timeInMillis ?: 0L
	// extra will create the translation effect properly
	val amount = if (differences > 0) differences + extra else +extra
	val result = buildList {
		addAll(this@padListWithExtra)
		repeat(amount) {
			val timeInMillis = lastValue + (it * bufferSize)
			add(RecordedPoint(timeInMillis, 0f, true))
		}
	}.distinctBy { it.timeInMillis }
	return result
}

internal fun List<RecordedPoint>.toProperSequence(eachBlockSize: Int): List<RecordedPoint> {
	if (isEmpty()) return emptyList()

	val resultList = mutableListOf<RecordedPoint>()
	var expected = first()
	var start = expected.timeInMillis

	for (actualPoint in this) {
		while (start < actualPoint.timeInMillis) {
			resultList.add(expected.copy(timeInMillis = start))
			start += eachBlockSize
		}
		resultList.add(actualPoint)
		expected = actualPoint
		start += eachBlockSize
	}

	return resultList
}

private fun lerp(v0: Float, v1: Float, t: Float): Float {
	return (1 - t) * v1 + t * v0
}