package com.eva.recorderapp.voice_recorder.data.util

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun LocalDateTime.toDuration(): Duration {
	val millis = toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
	return millis.toDuration(DurationUnit.MILLISECONDS)
}

fun LocalDateTime.toMillis(): Long {
	return toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}