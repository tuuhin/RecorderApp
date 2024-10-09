package com.eva.recorderapp.voice_recorder.widgets.data

import com.eva.recorderapp.voice_recorder.domain.recorder.emums.RecorderState
import kotlinx.datetime.LocalTime

fun RecorderWidgetDataProto.toModel(): RecorderWidgetModel = RecorderWidgetModel(
	state = when (mode) {
		RecordingModeProto.RECORDING -> RecorderState.RECORDING
		RecordingModeProto.PAUSED -> RecorderState.PAUSED
		else -> RecorderState.IDLE
	},
	time = LocalTime.fromSecondOfDay(duration.seconds.toInt())
)