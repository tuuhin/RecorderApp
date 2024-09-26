package com.eva.recorderapp.voice_recorder.data.util

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun LocalDateTime.toMillis(): Long {
	return toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}

fun LocalDateTime.toDuration(): Duration {
	return toMillis().toDuration(DurationUnit.MILLISECONDS)
}

fun LocalTime.roundToClosestSeconds(): LocalTime {
	return LocalTime.fromSecondOfDay(this.toSecondOfDay())
}

val Duration.asLocalTime: LocalTime
	get() = LocalTime.fromNanosecondOfDay(toLong(DurationUnit.NANOSECONDS))