package com.eva.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
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

fun Duration.toLocalDateTime(): LocalDateTime {
	return Instant.fromEpochMilliseconds(inWholeMilliseconds)
		.toLocalDateTime(TimeZone.currentSystemDefault())
}