package com.eva.recorderapp.voice_recorder.presentation.categories.create_category

import androidx.compose.ui.text.input.TextFieldValue
import com.eva.recorderapp.voice_recorder.domain.categories.models.CategoryColor
import com.eva.recorderapp.voice_recorder.domain.categories.models.CategoryType

data class CreateOrUpdateCategoryState(
	val textValue: TextFieldValue = TextFieldValue(),
	val error: String = "",
	val color: CategoryColor = CategoryColor.COLOR_UNKNOWN,
	val type: CategoryType = CategoryType.CATEGORY_NONE,
	val isEditMode: Boolean = false,
) {
	val hasError: Boolean
		get() = error.isNotBlank()


}
