package com.eva.feature_recordings.search.state

import com.eva.categories.domain.models.RecordingCategoryModel

internal sealed interface SearchRecordingScreenEvent {
	data class OnSelectTimeFilter(val filter: SearchFilterTimeOption?) : SearchRecordingScreenEvent
	data class OnCategorySelected(val category: RecordingCategoryModel?) :
		SearchRecordingScreenEvent

	data class OnVoiceSearchResults(val results: List<String>) : SearchRecordingScreenEvent
	data class OnQueryChange(val text: String) : SearchRecordingScreenEvent
}