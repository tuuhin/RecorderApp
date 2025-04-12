package com.eva.feature_widget.recorder.repository

import com.eva.recorder.domain.models.RecorderState
import kotlinx.datetime.LocalTime

interface RecorderWidgetRepo {
	suspend fun updateRecordingWidget(state: RecorderState, time: LocalTime?)
	suspend fun resetRecorderWidget()
}
