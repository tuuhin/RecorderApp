package com.eva.recorderapp.voice_recorder.presentation.categories.utils

import androidx.compose.ui.text.input.TextFieldValue

data class CreateOrEditCategoryState(
	val isEditMode: Boolean = false,
	val showSheet: Boolean = false,
	val textValue: TextFieldValue = TextFieldValue(),
	val error: String = "",
) {
	val hasError: Boolean
		get() = error.isNotBlank()
}
