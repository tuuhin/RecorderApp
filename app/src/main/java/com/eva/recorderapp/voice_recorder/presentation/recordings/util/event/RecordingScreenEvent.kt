package com.eva.recorderapp.voice_recorder.presentation.recordings.util.event

import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.SortOptions
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.SortOrder

sealed interface RecordingScreenEvent {

	data class OnRecordingSelectOrUnSelect(val recording: RecordedVoiceModel) : RecordingScreenEvent

	data object OnSelectAllRecordings : RecordingScreenEvent

	data object OnUnSelectAllRecordings : RecordingScreenEvent

	data object OnSelectedItemTrashRequest : RecordingScreenEvent

	data class OnSortOptionChange(val sort: SortOptions) : RecordingScreenEvent

	data class OnSortOrderChange(val order: SortOrder) : RecordingScreenEvent

	data object ShareSelectedRecordings : RecordingScreenEvent
}