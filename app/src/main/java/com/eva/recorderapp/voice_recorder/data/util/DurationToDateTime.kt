package com.eva.recorderapp.voice_recorder.data.util

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration

fun Duration.toLocalDateTime(): LocalDateTime {
	return Instant.fromEpochMilliseconds(inWholeMilliseconds)
		.toLocalDateTime(TimeZone.currentSystemDefault())
}

