package com.eva.recorderapp.voice_recorder.domain.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.concurrent.ConcurrentLinkedQueue

fun Flow<Int>.flowToFixedSizeCollection(maxSize: Int): Flow<List<Int>> {
	require(maxSize > 0) { "maxSize must be positive" }
	val buffer = ConcurrentLinkedQueue<Int>()
	return flow {
		try {
			collect { value ->
				if (buffer.size >= maxSize)
					buffer.poll()
				buffer.offer(value)
				val paddedList = if (maxSize - buffer.size > 0) {
					val extras = List(maxSize - buffer.size) { 0 }
					buffer.plus(extras).toList()
				} else buffer.toList()
				emit(paddedList)
			}
		} catch (e: CancellationException) {
			throw e
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}.flowOn(Dispatchers.Default)
}
