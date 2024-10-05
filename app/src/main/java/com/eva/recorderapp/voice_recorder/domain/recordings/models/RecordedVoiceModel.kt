package com.eva.recorderapp.voice_recorder.domain.recordings.models

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class RecordedVoiceModel(
	val id: Long,
	val title: String,
	val displayName: String,
	val mimeType: String,
	val modifiedAt: LocalDateTime,
	val recordedAt: LocalDateTime,
	val fileUri: String,
	val duration: Duration = 0.seconds,
	val sizeInBytes: Long = 0,
	val isFavorite: Boolean = false,
	val categoryId: Long? = null,
	val owner: String? = null,
) {
	val durationAsLocaltime: LocalTime
		get() = LocalTime.fromSecondOfDay(duration.inWholeSeconds.toInt())
}