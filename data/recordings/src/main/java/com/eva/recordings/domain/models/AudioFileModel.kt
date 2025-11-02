package com.eva.recordings.domain.models

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.time.Duration
import kotlin.time.DurationUnit

data class AudioFileModel(
	val id: Long,
	val displayName: String,
	val size: Long,
	val duration: Duration,
	val lastModified: LocalDateTime,
	val fileUri: String,
	val mimeType: String,
	val title: String = displayName,
	val path: String? = null,
	val isFavourite: Boolean = false,
	val metaData: MediaMetaDataInfo? = null,
) {
	val durationAsLocaltime: LocalTime
		get() = LocalTime.fromSecondOfDay(duration.toInt(DurationUnit.SECONDS))

}