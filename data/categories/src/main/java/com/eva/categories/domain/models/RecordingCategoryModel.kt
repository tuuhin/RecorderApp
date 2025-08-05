package com.eva.categories.domain.models

import kotlinx.datetime.LocalDateTime

data class RecordingCategoryModel(
	val id: Long,
	val name: String,
	val createdAt: LocalDateTime? = null,
	val categoryType: CategoryType = CategoryType.CATEGORY_NONE,
	val categoryColor: CategoryColor = CategoryColor.COLOR_UNKNOWN,
) {

	companion object {
		val ALL_CATEGORY = RecordingCategoryModel(
			id = -1,
			name = "All Recordings",
			createdAt = null
		)
	}
}
