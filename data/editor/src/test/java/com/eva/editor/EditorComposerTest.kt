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
			AudioClipConfig(3.seconds, 6.seconds) to AudioEditAction.CROP
		)
		val result = EditorComposer.applyLogicalEditSequence(total, edits)

		assertEquals(
			listOf(
				AudioClipConfig(3.seconds, 4.seconds),
				AudioClipConfig(6.seconds, 8.seconds)
			),
			result
		)
	}

	@Test
	fun `multiple cuts on the same clip`() {
		val edits = listOf(
			AudioClipConfig(2.seconds, 4.seconds) to AudioEditAction.CUT,
			AudioClipConfig(6.seconds, 8.seconds) to AudioEditAction.CUT
		)
		val result = EditorComposer.applyLogicalEditSequence(total, edits)
		assertEquals(
			listOf(
				AudioClipConfig(0.seconds, 2.seconds),
				AudioClipConfig(4.seconds, 6.seconds),
				AudioClipConfig(8.seconds, 10.seconds)
			),
			result
		)
	}

	@Test
	fun `combining a series of cut and crop`() {
		val edits = listOf(
			AudioClipConfig(6.seconds, 8.seconds) to AudioEditAction.CUT,
			AudioClipConfig(4.seconds, 10.seconds) to AudioEditAction.CROP,
			AudioClipConfig(2.seconds, 3.seconds) to AudioEditAction.CUT
		)
		val result = EditorComposer.applyLogicalEditSequence(total, edits)
		assertEquals(
			listOf(
				AudioClipConfig(4.seconds, 6.seconds),
				AudioClipConfig(9.seconds, 10.seconds)
			),
			result
		)
	}


}