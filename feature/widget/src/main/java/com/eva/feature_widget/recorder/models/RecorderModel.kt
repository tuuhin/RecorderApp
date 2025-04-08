package com.eva.feature_widget.recorder.models

import com.eva.recorder.domain.models.RecorderState
import kotlinx.datetime.LocalTime

internal data class RecorderModel(
	val state: RecorderState,
	val time: LocalTime,
)