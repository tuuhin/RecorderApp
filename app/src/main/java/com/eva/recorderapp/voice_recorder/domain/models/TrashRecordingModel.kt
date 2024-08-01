package com.eva.recorderapp.voice_recorder.domain.models

import kotlinx.datetime.LocalDateTime

data class TrashRecordingModel(
	val id: Long,
	val title: String,
	val displayName: String,
	val mimeType: String,
	val recordedAt: LocalDateTime,
	val fileUri: String,
	val expiresAt: LocalDateTime,
)