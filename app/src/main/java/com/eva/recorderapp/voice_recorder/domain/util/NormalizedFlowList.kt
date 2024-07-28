package com.eva.recorderapp.voice_recorder.domain.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

fun Flow<List<Int>>.toNormalizedValues(): Flow<FloatArray> = map { amplitudes ->
	var min = amplitudes.minOrNull() ?: 0
	var max = amplitudes.maxOrNull() ?: 0
	val range = max - min
	// if range is zero  return empty
	// that's probably the empty case
	if (range == 0) return@map floatArrayOf()

	val normalizedValue = amplitudes.map { amp ->
		val normalizedValue = (amp - min).toFloat() / range
		normalizedValue.coerceIn(0f..1f)
	}
	return@map normalizedValue.toFloatArray()

}.flowOn(Dispatchers.Default)
