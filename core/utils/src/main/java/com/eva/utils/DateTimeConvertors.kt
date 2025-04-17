package com.eva.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

const val HALF_OF_SECOND_IN_NANOS = 500_000_000


fun LocalTime.roundToClosestSeconds(): LocalTime {
	return if (nanosecond >= HALF_OF_SECOND_IN_NANOS)
		LocalTime(hour, minute, second + 1)
	else LocalTime(hour, minute, second)
}


fun Duration.toLocalDateTime(): LocalDateTime {
	return Instant.fromEpochMilliseconds(inWholeMilliseconds)
		.toLocalDateTime(TimeZone.currentSystemDefault())
}

fun Duration.toLocalTime(): LocalTime {
	return LocalTime(
		hour = inWholeHours.toInt(),
		minute = inWholeMinutes.toInt() % 60,
		second = inWholeSeconds.toInt() % 60
	)
}

fun LocalTime.toDuration(): Duration = toMillisecondOfDay().milliseconds
