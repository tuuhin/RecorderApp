package com.eva.recorderapp.voice_recorder.domain.models

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.time.Duration

data class RecordedVoiceModel(
	val id: Long,
	val title: String,
	val displayName: String,
	val duration: Duration,
	val sizeInBytes: Int,
	val modifiedAt: LocalDateTime,
	val recordedAt: LocalDateTime,
	val fileUri: String,
	val isTrashed: Boolean = false,
	val expiresAt: LocalDateTime? = null
) {
	val durationAsLocaltime: LocalTime
		get() = LocalTime.fromSecondOfDay(duration.inWholeSeconds.toInt())
}