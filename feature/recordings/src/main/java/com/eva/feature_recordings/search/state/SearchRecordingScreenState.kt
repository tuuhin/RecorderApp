package com.eva.feature_recordings.search.state

import com.eva.categories.domain.models.RecordingCategoryModel

internal data class SearchRecordingScreenState(
	val searchQuery: String = "",
	val selectedCategory: RecordingCategoryModel? = null,
	val timeFilter: SearchFilterTimeOption? = null,
)
