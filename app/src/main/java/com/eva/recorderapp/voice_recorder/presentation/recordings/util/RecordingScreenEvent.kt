package com.eva.recorderapp.voice_recorder.presentation.recordings.util

import com.eva.recorderapp.voice_recorder.domain.models.RecordedVoiceModel

sealed interface RecordingScreenEvent {

	data class OnRecordingSelectOrUnSelect(val recording: RecordedVoiceModel) : RecordingScreenEvent

	data object OnSelectAllRecordings : RecordingScreenEvent

	data object OnUnSelectAllRecordings : RecordingScreenEvent

	data object OnSelectedItemTrashRequest : RecordingScreenEvent

}