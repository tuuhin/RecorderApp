package com.eva.editor

import com.eva.editor.domain.EditorComposer
import com.eva.editor.domain.model.AudioClipConfig
import com.eva.editor.domain.model.AudioEditAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class EditorComposerTest {

    private val total = 10.seconds

    @Test
    fun `cut a normal section from a clip of 10 seconds`() {
        val edits = listOf(AudioClipConfig(3.seconds, 5.seconds) to AudioEditAction.CUT)
        val result = EditorComposer.applyLogicalEditSequence(total, edits)

        val expected =
            listOf(AudioClipConfig(0.seconds, 3.seconds), AudioClipConfig(5.seconds, 10.seconds))

        assertEquals(expected, result)
    }

    @Test
    fun `crop a normal section from a clip of 10 seconds`() {
        val edits = listOf(AudioClipConfig(3.seconds, 7.seconds) to AudioEditAction.CROP)
        val result = EditorComposer.applyLogicalEditSequence(total, edits)
        val expected = listOf(AudioClipConfig(3.seconds, 7.seconds))
        assertEquals(expected, result)
    }

    @Test
    fun `cut a section and then apply crop`() {
        val edits = listOf(
            AudioClipConfig(4.seconds, 6.seconds) to AudioEditAction.CUT,
            AudioClipConfig(3.seconds, 6.seconds) to AudioEditAction.CROP,
        )
        val result = EditorComposer.applyLogicalEditSequence(total, edits)

        assertEquals(
            listOf(
                AudioClipConfig(3.seconds, 4.seconds),
                AudioClipConfig(6.seconds, 8.seconds),
            ),
            result,
        )
    }

    @Test
    fun `multiple cuts on the same clip`() {
        val edits = listOf(
            AudioClipConfig(2.seconds, 4.seconds) to AudioEditAction.CUT,
            AudioClipConfig(4.seconds, 6.seconds) to AudioEditAction.CUT,
        )
        val result = EditorComposer.applyLogicalEditSequence(total, edits)
        assertEquals(
            listOf(
                AudioClipConfig(0.seconds, 2.seconds),
                AudioClipConfig(4.seconds, 6.seconds),
                AudioClipConfig(8.seconds, 10.seconds),
            ),
            result,
        )
    }

    @Test
    fun `combining a series of cut and crop`() {
        val edits = listOf(
            AudioClipConfig(6.seconds, 8.seconds) to AudioEditAction.CUT,
            AudioClipConfig(4.seconds, 10.seconds) to AudioEditAction.CROP,
            AudioClipConfig(2.seconds, 3.seconds) to AudioEditAction.CUT,
        )
        val result = EditorComposer.applyLogicalEditSequence(total, edits)
        assertEquals(
            listOf(
                AudioClipConfig(4.seconds, 6.seconds),
                AudioClipConfig(9.seconds, 10.seconds),
            ),
            result,
        )
    }

    @Test
    fun `no edits returns the full clip`() {
        val result = EditorComposer.applyLogicalEditSequence(total, emptyList())
        assertEquals(listOf(AudioClipConfig(0.seconds, 10.seconds)), result)
    }

    @Test
    fun `cut from the very start`() {
        val edits = listOf(AudioClipConfig(0.seconds, 3.seconds) to AudioEditAction.CUT)
        val result = EditorComposer.applyLogicalEditSequence(total, edits)
        assertEquals(listOf(AudioClipConfig(3.seconds, 10.seconds)), result)
    }

    @Test
    fun `cut up to the very end`() {
        val edits = listOf(AudioClipConfig(7.seconds, 10.seconds) to AudioEditAction.CUT)
        val result = EditorComposer.applyLogicalEditSequence(total, edits)
        assertEquals(listOf(AudioClipConfig(0.seconds, 7.seconds)), result)
    }

    @Test
    fun `second cut is relative to the already edited timeline`() {
        // after the first cut the timeline is orig 0..2 + 4..10 -> edited 0..8
        // edited 2..4 therefore maps to original 4..6
        val edits = listOf(
            AudioClipConfig(2.seconds, 4.seconds) to AudioEditAction.CUT,
            AudioClipConfig(2.seconds, 4.seconds) to AudioEditAction.CUT,
        )
        val result = EditorComposer.applyLogicalEditSequence(total, edits)
        assertEquals(
            listOf(
                AudioClipConfig(0.seconds, 2.seconds),
                AudioClipConfig(6.seconds, 10.seconds),
            ),
            result,
        )
    }

    @Test
    fun `crop after crop is relative to the first crop`() {
        // first crop -> orig 2..8, second crop edited 1..4 -> orig 3..6
        val edits = listOf(
            AudioClipConfig(2.seconds, 8.seconds) to AudioEditAction.CROP,
            AudioClipConfig(1.seconds, 4.seconds) to AudioEditAction.CROP,
        )
        val result = EditorComposer.applyLogicalEditSequence(total, edits)
        assertEquals(listOf(AudioClipConfig(3.seconds, 6.seconds)), result)
    }

    @Test
    fun `cut after crop maps into the original timeline`() {
        // crop -> orig 2..8; cut edited 2..4 -> orig 4..6
        val edits = listOf(
            AudioClipConfig(2.seconds, 8.seconds) to AudioEditAction.CROP,
            AudioClipConfig(2.seconds, 4.seconds) to AudioEditAction.CUT,
        )
        val result = EditorComposer.applyLogicalEditSequence(total, edits)
        assertEquals(
            listOf(
                AudioClipConfig(2.seconds, 4.seconds),
                AudioClipConfig(6.seconds, 8.seconds),
            ),
            result,
        )
    }

    @Test
    fun `crop end beyond the current length is clamped`() {
        val edits = listOf(AudioClipConfig(5.seconds, 15.seconds) to AudioEditAction.CROP)
        val result = EditorComposer.applyLogicalEditSequence(total, edits)
        assertEquals(listOf(AudioClipConfig(5.seconds, 10.seconds)), result)
    }

    @Test
    fun `millisecond precision is preserved`() {
        val edits = listOf(AudioClipConfig(1500, 2500) to AudioEditAction.CUT)
        val result = EditorComposer.applyLogicalEditSequence(total, edits)
        assertEquals(
            listOf(AudioClipConfig(0, 1500), AudioClipConfig(2500, 10_000)),
            result,
        )
    }
}
