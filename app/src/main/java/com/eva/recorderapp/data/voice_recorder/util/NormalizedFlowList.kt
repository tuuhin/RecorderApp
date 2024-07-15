package com.eva.recorderapp.data.voice_recorder.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

fun Flow<List<Int>>.toNormalizedValues(): Flow<List<Float>> = map { amplitudes ->
	var min = Int.MAX_VALUE
	var max = Int.MIN_VALUE
	for (amplitude in amplitudes) {
		min = minOf(min, amplitude)
		max = maxOf(max, amplitude)
	}
	val range = max - min
	// if range is zero  return empty
	if (range == 0) return@map List(amplitudes.size) { 0.0f }

	buildList {
		amplitudes.forEach { amp ->
			val normalizedValue = (amp - min).toFloat() / range
			add(normalizedValue.coerceIn(0f, 1f))
		}
	}
}.flowOn(Dispatchers.Default)
