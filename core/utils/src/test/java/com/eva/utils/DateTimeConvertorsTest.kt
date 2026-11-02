package com.eva.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.datetime.LocalTime

class DateTimeConvertorsTest {

    @Test
    fun `roundToClosestHundredMillis quantizes down to nearest 100ms bucket`() {
        val time = LocalTime(hour = 0, minute = 0, second = 1, nanosecond = 240_000_000)
        val rounded = time.roundToClosestHundredMillis()

        assertEquals(LocalTime(hour = 0, minute = 0, second = 1, nanosecond = 200_000_000), rounded)
        assertTrue(rounded <= time)
    }

    @Test
    fun `roundToClosestHundredMillis handles sub-100ms and boundary values`() {
        val time99ms = LocalTime(hour = 0, minute = 0, second = 1, nanosecond = 299_000_000)
        assertEquals(
            LocalTime(hour = 0, minute = 0, second = 1, nanosecond = 200_000_000),
            time99ms.roundToClosestHundredMillis(),
        )

        val exact100ms = LocalTime(hour = 0, minute = 0, second = 1, nanosecond = 200_000_000)
        assertEquals(exact100ms, exact100ms.roundToClosestHundredMillis())

        val zero = LocalTime(hour = 0, minute = 0, second = 0, nanosecond = 0)
        assertEquals(zero, zero.roundToClosestHundredMillis())

        val sub100ms = LocalTime(hour = 0, minute = 0, second = 0, nanosecond = 75_000_000)
        assertEquals(zero, sub100ms.roundToClosestHundredMillis())
    }

    @Test
    fun `roundToClosestMillis with custom bucket`() {
        val time = LocalTime(hour = 1, minute = 2, second = 3, nanosecond = 456_000_000)
        val rounded50ms = time.roundToClosestMillis(50)
        assertEquals(
            LocalTime(hour = 1, minute = 2, second = 3, nanosecond = 450_000_000),
            rounded50ms,
        )
    }

    @Test
    fun `toDuration produces exact duration from LocalTime`() {
        val time = LocalTime(hour = 0, minute = 0, second = 2, nanosecond = 300_000_000)
        val duration = time.toDuration()
        assertEquals(2300.milliseconds, duration)
    }
}
