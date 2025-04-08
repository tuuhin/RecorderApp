package com.eva.feature_categories.create_category

import androidx.compose.ui.text.input.TextFieldValue
import com.eva.categories.domain.models.CategoryColor
import com.eva.categories.domain.models.CategoryType

internal sealed interface CreateCategoryEvent {

	data class OnTextFieldValueChange(val value: TextFieldValue) : CreateCategoryEvent

	data object OnCreateOrEditCategory : CreateCategoryEvent

	data class OnCategoryColorSelect(val color: CategoryColor) : CreateCategoryEvent

	data class OnCategoryTypeChange(val type: CategoryType) : CreateCategoryEvent
}