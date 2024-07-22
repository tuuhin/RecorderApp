package com.eva.recorderapp.voice_recorder.presentation.recordings.util

import com.eva.recorderapp.voice_recorder.domain.models.RecordedVoiceModel

interface TrashScreenEvent {

	data class OnRecordingSelectOrUnSelect(
		val recording: RecordedVoiceModel
	) : TrashScreenEvent

	data object OnSelectTrashRecording : TrashScreenEvent

	data object OnUnSelectTrashRecording : TrashScreenEvent

	data object OnSelectItemRestore : TrashScreenEvent

	data object OnSelectItemDeleteForeEver : TrashScreenEvent
}