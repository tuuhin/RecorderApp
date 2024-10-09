package com.eva.recorderapp.voice_recorder.domain.util

import com.eva.recorderapp.voice_recorder.domain.recorder.emums.RecorderState
import kotlinx.datetime.LocalTime

interface AppWidgetsRepository {

	/**
	 * Updates Recordings widget with the current data
	 */
	suspend fun recordingsWidgetUpdate()

	/**
	 * Updates the recorder widget with [RecorderState] and stopwatch time as [LocalTime]
	 */
	suspend fun recorderWidgetUpdate(state: RecorderState, time: LocalTime? = null)

	/**
	 * Reset the recorder widget to [RecorderState.IDLE]
	 */
	suspend fun resetRecorderWidget()
}