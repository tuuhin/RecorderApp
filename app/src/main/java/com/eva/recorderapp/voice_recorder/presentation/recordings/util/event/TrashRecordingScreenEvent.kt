package com.eva.recorderapp.voice_recorder.presentation.recordings.util.event

import com.eva.recorderapp.voice_recorder.domain.recordings.models.TrashRecordingModel

interface TrashRecordingScreenEvent {

	data class OnRecordingSelectOrUnSelect(
		val recording: TrashRecordingModel
	) : TrashRecordingScreenEvent

	data object OnSelectTrashRecording : TrashRecordingScreenEvent

	data object OnUnSelectTrashRecording : TrashRecordingScreenEvent

	data object OnSelectItemRestore : TrashRecordingScreenEvent

	data object OnSelectItemDeleteForeEver : TrashRecordingScreenEvent
}