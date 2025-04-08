package com.eva.feature_categories.category_picker

import com.eva.categories.domain.models.RecordingCategoryModel

internal sealed interface CategoryPickerEvent {

	data class SelectCategory(val category: RecordingCategoryModel) : CategoryPickerEvent

	data object OnSetRecordingCategory : CategoryPickerEvent
}