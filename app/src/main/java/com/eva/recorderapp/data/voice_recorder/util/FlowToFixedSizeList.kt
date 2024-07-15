package com.eva.recorderapp.data.voice_recorder.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

fun Flow<Int>.flowToFixedSizeCollection(maxSize: Int): Flow<List<Int>> {
	require(maxSize > 0) { "maxSize must be positive" }
	val buffer = mutableListOf<Int>()
	return flow {
		collect { value ->
			buffer.add(value)
			if (buffer.size > maxSize) {
				buffer.removeAt(0)
			}
			val extras = List(maxSize - buffer.size) { buffer.getOrElse(0, { 0 }) }
			val paddedList = buffer.plus(extras)
			emit(paddedList) // Emit a copy of the latest elements
		}
	}.flowOn(Dispatchers.Default)
}
