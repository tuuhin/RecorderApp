package com.eva.recorderapp.voice_recorder.domain.categories.models

import kotlinx.datetime.LocalDateTime

data class RecordingCategoryModel(
	val id: Long,
	val name: String,
	val createdAt: LocalDateTime? = null,
	val count: Long = 0L,
	val categoryType: CategoryType = CategoryType.CATEGORY_NONE,
	val categoryColor: CategoryColor = CategoryColor.COLOR_UNKNOWN,
) {

	val hasCount: Boolean
		get() = count > 0

	companion object {
		val ALL_CATEGORY = RecordingCategoryModel(
			id = -1,
			name = "All Recordings",
			createdAt = null
		)
	}
}
