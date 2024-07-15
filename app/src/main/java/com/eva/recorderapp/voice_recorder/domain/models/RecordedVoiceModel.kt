package com.eva.recorderapp.voice_recorder.domain.models

import kotlinx.datetime.LocalDateTime

data class RecordedVoiceModel(
	val id: Long,
	val title: String,
	val displayName: String,
	val duration: Long,
	val sizeInBytes: Long,
	val modifiedAt: LocalDateTime,
	val recordedAt: LocalDateTime,
	val fileUri: String
)
