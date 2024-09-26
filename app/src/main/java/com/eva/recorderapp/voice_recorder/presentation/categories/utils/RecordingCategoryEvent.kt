package com.eva.recorderapp.voice_recorder.presentation.categories.utils

import com.eva.recorderapp.voice_recorder.domain.categories.models.RecordingCategoryModel

sealed interface RecordingCategoryEvent {

	data class SelectCategory(val category: RecordingCategoryModel) : RecordingCategoryEvent

	data object OnSetRecordingCategory : RecordingCategoryEvent
}