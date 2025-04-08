package com.eva.feature_widget.recorder.models

import com.eva.feature_widget.proto.RecorderWidgetDataProto
import com.eva.feature_widget.proto.RecordingModeProto
import com.eva.recorder.domain.models.RecorderState
import kotlinx.datetime.LocalTime

internal fun RecorderWidgetDataProto.toModel(): RecorderModel = RecorderModel(
	state = when (mode) {
		RecordingModeProto.RECORDING -> RecorderState.RECORDING
		RecordingModeProto.PAUSED -> RecorderState.PAUSED
		else -> RecorderState.IDLE
	},
	time = LocalTime.fromSecondOfDay(duration.seconds.toInt())
)