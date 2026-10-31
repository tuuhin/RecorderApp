package com.eva.utils

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private const val NANOS_PER_SECOND = 1_000_000_000
private const val MINUTES_PER_HOUR = 60L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = MINUTES_PER_HOUR * SECONDS_PER_MINUTE
private const val SECONDS_PER_DAY = 24 * SECONDS_PER_HOUR

internal fun LocalTime.roundToClosestMillis(bucket: Int = 100): LocalTime {
    require(bucket > 0) { "Bucket size must be greater than 0" }
    val totalMillis = toMillisecondOfDay()
    val roundedMillis = (totalMillis / bucket) * bucket
    return LocalTime.fromMillisecondOfDay(roundedMillis)
}

fun LocalTime.roundToClosestHundredMillis(): LocalTime = roundToClosestMillis(100)

fun LocalTime.roundToClosestSeconds(): LocalTime {
    val roundedSecond = second + 1
    return when {
        roundedSecond < 60 -> LocalTime(hour, minute, roundedSecond)
        minute < 59 -> LocalTime(hour, minute + 1, 0)
        hour < 23 -> LocalTime(hour + 1, 0, 0)
        else -> LocalTime(0, 0, 0)
    }
}

fun Duration.toLocalDateTime(): LocalDateTime =
    Instant.fromEpochMilliseconds(inWholeMilliseconds)
        .toLocalDateTime(TimeZone.currentSystemDefault())

fun Duration.toLocalTime(): LocalTime {
    val totalSeconds = inWholeSeconds
    val secondsOfDay = totalSeconds % SECONDS_PER_DAY

    return LocalTime(
        hour = (secondsOfDay / SECONDS_PER_HOUR).toInt(),
        minute = (secondsOfDay / SECONDS_PER_MINUTE % MINUTES_PER_HOUR).toInt(),
        second = (secondsOfDay % SECONDS_PER_MINUTE).toInt(),
        nanosecond = (inWholeNanoseconds % NANOS_PER_SECOND).toInt(),
    )
}

fun LocalTime.toDuration(): Duration = toMillisecondOfDay().milliseconds
