package com.eva.feature_categories.create_category

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.input.TextFieldValue
import com.eva.categories.domain.models.CategoryColor
import com.eva.categories.domain.models.CategoryType
import com.eva.categories.domain.models.RecordingCategoryModel

@Stable
internal data class CreateCategoryState(
	val textValue: TextFieldValue = TextFieldValue(),
	val error: String = "",
	val color: CategoryColor = CategoryColor.COLOR_UNKNOWN,
	val type: CategoryType = CategoryType.CATEGORY_NONE,
	val isEditMode: Boolean = false,
) {
	val hasError: Boolean
		get() = error.isNotBlank()

}

internal fun CreateCategoryState.toModel(categoryId: Long): RecordingCategoryModel {
	return RecordingCategoryModel(
		id = categoryId,
		categoryType = type,
		categoryColor = color,
		name = textValue.text
	)
}
