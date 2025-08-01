package com.eva.categories.data

import com.eva.categories.domain.models.CategoryColor
import com.eva.categories.domain.models.CategoryType
import com.eva.categories.domain.models.RecordingCategoryModel
import com.eva.database.entity.RecordingCategoryEntity
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.LocalDateTime

internal fun RecordingCategoryEntity.toModel(): RecordingCategoryModel =
	RecordingCategoryModel(
		id = categoryId ?: 0L,
		name = categoryName,
		createdAt = createdAt,
		categoryColor = toCategoryColor(color),
		categoryType = toCategoryType(type),
	)

internal fun RecordingCategoryModel.toEntity(): RecordingCategoryEntity =
	RecordingCategoryEntity(
		categoryId = id,
		categoryName = name,
		createdAt = createdAt ?: LocalDateTime.now().toKotlinLocalDateTime(),
		color = categoryColor.name,
		type = categoryType.name,
	)

private fun toCategoryType(name: String?): CategoryType = name?.let {
	try {
		CategoryType.valueOf(name)
	} catch (_: IllegalArgumentException) {
		CategoryType.CATEGORY_NONE
	}
} ?: CategoryType.CATEGORY_NONE

private fun toCategoryColor(name: String?): CategoryColor = name?.let {
	try {
		CategoryColor.valueOf(name)
	} catch (_: IllegalArgumentException) {
		CategoryColor.COLOR_UNKNOWN
	}
} ?: CategoryColor.COLOR_UNKNOWN
