package com.eva.recorderapp.voice_recorder.domain.player.model

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.time.Duration
import kotlin.time.DurationUnit

data class AudioFileModel(
	val id: Long,
	val title: String,
	val displayName: String,
	val size: Long,
	val duration: Duration,
	val lastModified: LocalDateTime,
	val channel: Int,
	val bitRateInKbps: Float,
	val samplingRateKHz: Float,
	val path: String,
	val fileUri: String,
	val mimeType: String,
	val isFavourite: Boolean = false,
	val metaDataLocation: String = "",
) {
	val durationAsLocaltime: LocalTime
		get() = LocalTime.fromSecondOfDay(duration.toInt(DurationUnit.SECONDS))

	val hasLocation: Boolean
		get() = metaDataLocation.isNotBlank()

}
