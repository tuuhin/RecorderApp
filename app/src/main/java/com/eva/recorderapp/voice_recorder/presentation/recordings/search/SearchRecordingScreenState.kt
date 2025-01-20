package com.eva.recorderapp.voice_recorder.presentation.recordings.search

import com.eva.recorderapp.voice_recorder.domain.categories.models.RecordingCategoryModel

data class SearchRecordingScreenState(
	val searchQuery: String = "",
	val selectedCategory: RecordingCategoryModel? = null,
	val timeFilter: SearchFilterTimeOption? = null,
)
