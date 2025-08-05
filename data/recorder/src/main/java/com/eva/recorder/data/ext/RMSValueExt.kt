package com.eva.recorder.data.ext

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.sqrt

internal suspend fun rms(array: ShortArray): Float {
	return withContext(Dispatchers.Default) {
		val squaredSum = array.sumOf { it.toDouble().pow(2) }
		val avg = squaredSum / array.size
		sqrt(avg).toFloat()
	}
}