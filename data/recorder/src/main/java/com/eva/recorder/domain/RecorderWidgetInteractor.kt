package com.eva.recorder.domain

import com.eva.recorder.domain.models.RecorderState
import kotlinx.datetime.LocalTime

interface RecorderWidgetInteractor {

	/**
	 * Updates the recorder widget with [RecorderState] and stopwatch time as [LocalTime]
	 */
	suspend fun updateWidget(state: RecorderState, time: LocalTime? = null)

	/**
	 * Reset the recorder widget to [RecorderState.IDLE]
	 */
	suspend fun resetWidget()
}