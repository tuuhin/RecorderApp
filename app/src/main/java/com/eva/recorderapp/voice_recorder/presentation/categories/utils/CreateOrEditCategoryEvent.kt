package com.eva.recorderapp.voice_recorder.presentation.categories.utils

import androidx.compose.ui.text.input.TextFieldValue

sealed interface CreateOrEditCategoryEvent {

	data class OnTextFieldValueChange(val value: TextFieldValue) : CreateOrEditCategoryEvent

	data object OnAcceptChanges : CreateOrEditCategoryEvent

	data object OnDismissSheet : CreateOrEditCategoryEvent

	data object OnOpenSheetToCreate : CreateOrEditCategoryEvent

	data object OnOpenSheetToEdit : CreateOrEditCategoryEvent
}