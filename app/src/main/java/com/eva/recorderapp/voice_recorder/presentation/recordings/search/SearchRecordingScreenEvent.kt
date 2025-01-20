package com.eva.recorderapp.voice_recorder.presentation.recordings.search

import com.eva.recorderapp.voice_recorder.domain.categories.models.RecordingCategoryModel

sealed interface SearchRecordingScreenEvent {
	data class OnSelectTimeFilter(val filter: SearchFilterTimeOption?) : SearchRecordingScreenEvent
	data class OnCategorySelected(val category: RecordingCategoryModel?) : SearchRecordingScreenEvent

	data class OnQueryChange(val text: String) : SearchRecordingScreenEvent
}