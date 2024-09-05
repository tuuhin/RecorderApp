package com.eva.recorderapp.voice_recorder.presentation.categories.utils

import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordingCategoryModel

data class SelectableCategory(
	val category: RecordingCategoryModel,
	val isSelected: Boolean = false,
)

fun RecordingCategoryModel.toSelectableCategory(): SelectableCategory =
	SelectableCategory(category = this)

fun Collection<RecordingCategoryModel>.toSelectableCategories(): List<SelectableCategory> =
	map(RecordingCategoryModel::toSelectableCategory)
