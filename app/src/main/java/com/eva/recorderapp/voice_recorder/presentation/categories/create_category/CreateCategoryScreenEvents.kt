package com.eva.recorderapp.voice_recorder.presentation.categories.create_category

import androidx.compose.ui.text.input.TextFieldValue
import com.eva.recorderapp.voice_recorder.domain.categories.models.CategoryColor
import com.eva.recorderapp.voice_recorder.domain.categories.models.CategoryType

sealed interface CreateCategoryScreenEvents {

	data class OnTextFieldValueChange(val value: TextFieldValue) : CreateCategoryScreenEvents

	data object OnCreateOrEditCategory : CreateCategoryScreenEvents

	data class OnCategoryColorSelect(val color: CategoryColor) : CreateCategoryScreenEvents

	data class OnCategoryTypeChange(val type: CategoryType) : CreateCategoryScreenEvents
}