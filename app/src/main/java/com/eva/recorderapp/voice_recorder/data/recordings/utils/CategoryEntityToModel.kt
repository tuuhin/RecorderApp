package com.eva.recorderapp.voice_recorder.data.recordings.utils

import com.eva.recorderapp.voice_recorder.data.database.entity.RecordingCategoryEntity
import com.eva.recorderapp.voice_recorder.domain.categories.models.CategoryColor
import com.eva.recorderapp.voice_recorder.domain.categories.models.CategoryType
import com.eva.recorderapp.voice_recorder.domain.categories.models.RecordingCategoryModel
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.LocalDateTime

fun RecordingCategoryEntity.toModel(): RecordingCategoryModel =
	RecordingCategoryModel(
		id = categoryId ?: 0L,
		name = categoryName,
		createdAt = createdAt,
		categoryColor = color ?: CategoryColor.COLOR_UNKNOWN,
		categoryType = type ?: CategoryType.CATEGORY_NONE,
	)

fun RecordingCategoryEntity.toModel(count: Long): RecordingCategoryModel =
	RecordingCategoryModel(
		id = categoryId ?: 0L,
		name = categoryName,
		createdAt = createdAt,
		count = count,
		categoryColor = color ?: CategoryColor.COLOR_UNKNOWN,
		categoryType = type ?: CategoryType.CATEGORY_NONE,
	)

fun RecordingCategoryModel.toEntity(): RecordingCategoryEntity =
	RecordingCategoryEntity(
		categoryId = id,
		categoryName = name,
		createdAt = createdAt ?: LocalDateTime.now().toKotlinLocalDateTime(),
		color = categoryColor,
		type = categoryType
	)