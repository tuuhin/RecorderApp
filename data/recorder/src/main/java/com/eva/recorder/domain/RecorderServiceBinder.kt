package com.eva.recorder.domain

import com.eva.recorder.domain.models.RecordedPoint
import com.eva.recorder.domain.models.RecorderState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalTime
import kotlin.time.Duration

interface RecorderServiceBinder {

	val isConnectionReady: StateFlow<Boolean>

	val recorderTimer: Flow<LocalTime>

	val recorderState: Flow<RecorderState>

	val bookMarkTimes: Flow<Set<Duration>>

	val amplitudes: Flow<List<RecordedPoint>>

	fun bindToService()

	fun unBindService()

	fun cleanUp()
}