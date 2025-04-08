package com.eva.feature_categories.manage_category

import com.eva.categories.domain.models.RecordingCategoryModel

internal sealed interface ManageCategoriesEvent {

	data class OnDeleteCategory(val category: RecordingCategoryModel) : ManageCategoriesEvent

}