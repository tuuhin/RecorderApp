package com.eva.editor

import com.eva.editor.domain.model.AudioClipConfig
import junit.framework.TestCase.assertFalse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class AudioClipConfigTest {

    private val total = 10.seconds

    @Test
    fun `valid clip in the middle`() {
        assertTrue(AudioClipConfig(2.seconds, 5.seconds).validate(total))
    }

    @Test
    fun `clip starting at zero is valid`() {
        // fails with the current isPositive() check
        assertTrue(AudioClipConfig(0.seconds, 3.seconds).validate(total))
    }

    @Test
    fun `clip ending exactly at total is valid`() {
        assertTrue(AudioClipConfig(5.seconds, 10.seconds).validate(total))
    }

    @Test
    fun `clip ending after total is invalid`() {
        assertFalse(AudioClipConfig(5.seconds, 11.seconds).validate(total))
    }

    @Test
    fun `negative start is invalid`() {
        assertFalse(AudioClipConfig((-1).seconds, 3.seconds).validate(total))
    }

    @Test
    fun `clip exactly at minimum duration is valid`() {
        assertTrue(AudioClipConfig(2.seconds, 3.seconds).validate(total))
    }

    @Test
    fun `clip shorter than minimum duration is invalid`() {
        assertFalse(AudioClipConfig(2000, 2999).validate(total))
    }

    @Test
    fun `inverted range is invalid`() {
        assertFalse(AudioClipConfig(5.seconds, 3.seconds).validate(total))
    }

    @Test
    fun `millisecond constructor converts correctly`() {
        assertEquals(AudioClipConfig(1.seconds, 2.seconds), AudioClipConfig(1000, 2000))
    }
}
