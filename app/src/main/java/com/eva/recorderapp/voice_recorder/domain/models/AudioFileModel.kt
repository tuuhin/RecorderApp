package com.eva.recorderapp.voice_recorder.domain.models

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.time.Duration

data class AudioFileModel(
	val id: Long,
	val title: String,
	val displayName: String,
	val size: Long,
	val duration: Duration,
	val lastModified: LocalDateTime,
	val channel: Int,
	val bitRateInKbps: Float,
	val samplingRatekHz: Float,
	val path: String,
	val fileUri: String,
	val mimeType: String,
) {
	val durationAsLocaltime: LocalTime
		get() = LocalTime.fromSecondOfDay(duration.inWholeSeconds.toInt())

}
