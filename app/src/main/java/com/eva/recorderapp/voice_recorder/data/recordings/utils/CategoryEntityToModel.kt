package com.eva.recorderapp.voice_recorder.data.recordings.utils

import com.eva.recorderapp.voice_recorder.data.recordings.database.entity.RecordingCategoryEntity
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordingCategoryModel
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.LocalDateTime

fun RecordingCategoryEntity.toModel(): RecordingCategoryModel =
	RecordingCategoryModel(id = categoryId ?: 0L, name = categoryName, createdAt = createdAt)

fun RecordingCategoryEntity.toModel(count: Long): RecordingCategoryModel =
	RecordingCategoryModel(
		id = categoryId ?: 0L,
		name = categoryName,
		createdAt = createdAt,
		count = count
	)

fun RecordingCategoryModel.toEntity(): RecordingCategoryEntity =
	RecordingCategoryEntity(
		categoryId = id,
		categoryName = name,
		createdAt = createdAt ?: LocalDateTime.now().toKotlinLocalDateTime()
	)