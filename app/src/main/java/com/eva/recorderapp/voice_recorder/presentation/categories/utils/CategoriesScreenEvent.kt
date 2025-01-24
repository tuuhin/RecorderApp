package com.eva.recorderapp.voice_recorder.presentation.categories.utils

import com.eva.recorderapp.voice_recorder.domain.categories.models.RecordingCategoryModel

sealed interface CategoriesScreenEvent {

	data class OnDeleteCategory(val category: RecordingCategoryModel) : CategoriesScreenEvent

}