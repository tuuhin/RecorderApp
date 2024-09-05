package com.eva.recorderapp.voice_recorder.domain.recordings.models

import kotlinx.datetime.LocalDateTime

data class RecordingCategoryModel(
	val id: Long,
	val name: String,
	val createdAt: LocalDateTime ,
)
