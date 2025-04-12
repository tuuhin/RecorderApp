package com.eva.recorder.data.reader

internal data class AmplitudeRange(
	val max: Int = 0,
	val min: Int = 0,
) {
	val range: Int
		get() = (max - min).let { range -> if (range <= 0) 1 else range }
}
