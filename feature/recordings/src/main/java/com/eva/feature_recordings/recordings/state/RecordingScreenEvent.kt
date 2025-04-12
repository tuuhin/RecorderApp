package com.eva.feature_recordings.recordings.state

import com.eva.categories.domain.models.RecordingCategoryModel
import com.eva.recordings.domain.models.RecordedVoiceModel

internal sealed interface RecordingScreenEvent {

	data object PopulateRecordings : RecordingScreenEvent

	data class OnRecordingSelectOrUnSelect(val recording: RecordedVoiceModel) : RecordingScreenEvent

	data object OnSelectAllRecordings : RecordingScreenEvent

	data object OnUnSelectAllRecordings : RecordingScreenEvent

	data object OnSelectedItemTrashRequest : RecordingScreenEvent

	data object OnToggleFavourites : RecordingScreenEvent

	data class OnSortOptionChange(val sort: SortOptions) : RecordingScreenEvent

	data class OnSortOrderChange(val order: SortOrder) : RecordingScreenEvent

	data object ShareSelectedRecordings : RecordingScreenEvent

	data class OnPostTrashRequest(val message: String = "") : RecordingScreenEvent

	data class OnCategoryChanged(val categoryModel: RecordingCategoryModel) : RecordingScreenEvent
}