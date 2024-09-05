package com.eva.recorderapp.voice_recorder.presentation.categories.utils

import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordingCategoryModel

sealed interface CategoriesScreenEvent {

	data class OnToggleSelection(val model: RecordingCategoryModel) : CategoriesScreenEvent

	data object OnSelectAll : CategoriesScreenEvent

	data object OnUnSelectAll : CategoriesScreenEvent

	data object OnDeleteSelected : CategoriesScreenEvent

}