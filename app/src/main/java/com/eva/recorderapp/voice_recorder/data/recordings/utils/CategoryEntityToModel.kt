package com.eva.recorderapp.voice_recorder.data.recordings.utils

import com.eva.recorderapp.voice_recorder.data.recordings.database.entity.RecordingCategoryEntity
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordingCategoryModel

fun RecordingCategoryEntity.toModel(): RecordingCategoryModel =
	RecordingCategoryModel(categoryId ?: 0L, categoryName, createdAt)

fun RecordingCategoryModel.toEntity(): RecordingCategoryEntity =
	RecordingCategoryEntity(categoryId = id, categoryName = name, createdAt = createdAt)