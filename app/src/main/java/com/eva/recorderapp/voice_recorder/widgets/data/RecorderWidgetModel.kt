package com.eva.recorderapp.voice_recorder.widgets.data

import com.eva.recorderapp.voice_recorder.domain.recorder.emums.RecorderState
import kotlinx.datetime.LocalTime

data class RecorderWidgetModel(
	val state: RecorderState,
	val time: LocalTime,
)