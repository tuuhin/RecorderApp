package com.eva.recorderapp.voice_recorder.presentation.recordings.util.event

import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordingCategoryModel
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.SortOptions
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.SortOrder

sealed interface RecordingScreenEvent {

	data object PopulateRecordings : RecordingScreenEvent

	data class OnRecordingSelectOrUnSelect(val recording: RecordedVoiceModel) : RecordingScreenEvent

	data object OnSelectAllRecordings : RecordingScreenEvent

	data object OnUnSelectAllRecordings : RecordingScreenEvent

	data object OnSelectedItemTrashRequest : RecordingScreenEvent

	data object OnToggleFavourites : RecordingScreenEvent

	data class OnSortOptionChange(val sort: SortOptions) : RecordingScreenEvent

	data class OnSortOrderChange(val order: SortOrder) : RecordingScreenEvent

	data object ShareSelectedRecordings : RecordingScreenEvent

	data class OnPostTrashRequestApi30(
		val isSuccess: Boolean = false,
		val message: String = "",
	) : RecordingScreenEvent

	data class OnCategoryChanged(val categoryModel: RecordingCategoryModel) : RecordingScreenEvent
}