package com.eva.recorder.data.recorder

import com.eva.recorder.domain.models.RecordedPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecordedPointExtTest {

    @Test
    fun `given one recent point when padding window then extra points follow point`() {
        val point = RecordedPoint(timeInMillis = 12_000L, rmsValue = .6f)
        val result = listOf(point).padListWithExtra(targetSize = 4, blockSize = 1_000, extra = 0)

        assertEquals(3, result.size)
        assertEquals(point, result.first())
        assertFalse(result.first().isPaddingPoint)
        assertTrue(result.drop(1).all { it.isPaddingPoint })
        assertEquals(listOf(12_000L, 13_000L, 14_000L), result.map { it.timeInMillis })
    }

    @Test
    fun `given full window when padding then extra points follow existing samples`() {
        val points = listOf(
            RecordedPoint(timeInMillis = 1_000L, rmsValue = .2f),
            RecordedPoint(timeInMillis = 2_000L, rmsValue = .4f),
        )
        val result = points.padListWithExtra(targetSize = 2, blockSize = 1_000, extra = 2)

        assertEquals(listOf(1_000L, 2_000L, 3_000L), result.map { it.timeInMillis })
        assertEquals(points, result.take(2))
        assertTrue(result.last().isPaddingPoint)
    }

    @Test
    fun `given sequential timestamps when reconstructing sequence then preserve chronological points`() {
        val points = listOf(
            RecordedPoint(timeInMillis = 1_000L, rmsValue = .1f),
            RecordedPoint(timeInMillis = 2_000L, rmsValue = .2f),
        )

        assertEquals(points, points.toProperSequence(eachBlockSize = 1_000))
    }

    @Test
    fun `given irregular intervals when reconstructing sequence then fill missing slots chronologically`() {
        val points = listOf(
            RecordedPoint(timeInMillis = 1_000L, rmsValue = .1f),
            RecordedPoint(timeInMillis = 3_500L, rmsValue = .7f),
        )

        val result = points.toProperSequence(eachBlockSize = 1_000)

        assertEquals(listOf(1_000L, 2_000L, 3_000L, 3_500L), result.map { it.timeInMillis })
    }

    @Test
    fun `given out of order timestamps when reconstructing sequence then throw illegal state`() {
        val points = listOf(
            RecordedPoint(timeInMillis = 1_000L, rmsValue = .1f),
            RecordedPoint(timeInMillis = 3_000L, rmsValue = .7f),
            RecordedPoint(timeInMillis = 2_000L, rmsValue = .4f),
        )

        assertFailsWith<IllegalStateException> { points.toProperSequence(eachBlockSize = 1_000) }
    }

    @Test
    fun `given duplicate timestamps when reconstructing sequence then throw illegal state`() {
        val points = listOf(
            RecordedPoint(timeInMillis = 1_000L, rmsValue = .1f),
            RecordedPoint(timeInMillis = 1_000L, rmsValue = .7f),
            RecordedPoint(timeInMillis = 2_000L, rmsValue = .4f),
        )

        assertFailsWith<IllegalStateException> { points.toProperSequence(eachBlockSize = 1_000) }
    }

    @Test
    fun `given non positive block size when reconstructing sequence then throw illegal state`() {
        assertFailsWith<IllegalStateException>() { emptyList<RecordedPoint>().toProperSequence(eachBlockSize = 0) }
    }
}
