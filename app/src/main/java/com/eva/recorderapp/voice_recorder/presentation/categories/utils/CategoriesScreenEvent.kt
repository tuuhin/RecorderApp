package com.eva.recorderapp.voice_recorder.presentation.categories.utils

import com.eva.recorderapp.voice_recorder.domain.categories.models.RecordingCategoryModel

sealed interface CategoriesScreenEvent {

	data class OnToggleSelection(val category: RecordingCategoryModel) : CategoriesScreenEvent

	data object OnSelectAll : CategoriesScreenEvent

	data object OnUnSelectAll : CategoriesScreenEvent

	data object OnDeleteSelected : CategoriesScreenEvent

}