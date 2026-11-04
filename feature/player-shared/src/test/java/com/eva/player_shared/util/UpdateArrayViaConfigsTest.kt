package com.eva.player_shared.util

import android.util.Log
import com.eva.editor.domain.model.AudioClipConfig
import com.eva.editor.domain.model.AudioEditAction
import io.mockk.every
import io.mockk.mockkStatic
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals

class UpdateArrayViaConfigsTest {

	private val timePerBlock = 100

	@BeforeTest
	fun setup() {
		mockkStatic(Log::class)
		every { Log.d(any(), any()) } returns 0
		every { Log.i(any(), any()) } returns 0
		every { Log.e(any(), any()) } returns 0
	}

	@Test
	fun `cut removes the middle section`() = runTest {
		val original = FloatArray(10)
		val configs = listOf(
			AudioClipConfig(2 * timePerBlock, 5 * timePerBlock) to AudioEditAction.CUT
		)
		val result = original.updateArrayViaConfigs(configs, timePerBlock)
		val expectedSize = FloatArray(7)
		assertArrayEquals(expectedSize, result, 0.0f)
	}

	@Test
	fun `crop removes the boundary`() = runTest {
		val original = FloatArray(10)
		val configs = listOf(
			AudioClipConfig(2 * timePerBlock, 5 * timePerBlock) to AudioEditAction.CROP
		)
		val result = original.updateArrayViaConfigs(configs, timePerBlock)
		val expected = FloatArray(3)
		assertArrayEquals(expected, result, 0.0f)
	}

	@Test(expected = IllegalStateException::class)
	fun `range is reveres should throw an exception`() = runTest {
		val original = FloatArray(3)
		val configs = listOf(
			AudioClipConfig(5 * timePerBlock, 2 * timePerBlock) to AudioEditAction.CUT
		)
		original.updateArrayViaConfigs(configs, timePerBlock)
	}

	@Test
	fun `empty configs don't change the array content`() = runTest {
		val original = FloatArray(10)
		val configs = emptyList<Pair<AudioClipConfig, AudioEditAction>>()

		val result = original.updateArrayViaConfigs(configs, timePerBlock)
		assertArrayEquals(original, result, 0.0f)
	}

	@Test
	fun `cut then cut then crop`() = runTest {
		val original = FloatArray(10)
		val configs = listOf(
			AudioClipConfig(timePerBlock, 3 * timePerBlock) to AudioEditAction.CUT,
			AudioClipConfig(5 * timePerBlock, 7 * timePerBlock) to AudioEditAction.CUT,
			AudioClipConfig(timePerBlock, 4 * timePerBlock) to AudioEditAction.CROP
		)
		val result = original.updateArrayViaConfigs(configs, timePerBlock)

		val expected = FloatArray(3)
		assertArrayEquals(expected, result, 0.0f)
	}

	@Test
	fun `cut from front and cut from back`() = runTest {
		val original = FloatArray(10) { it.toFloat() }

		val configs = listOf(
			AudioClipConfig(7 * timePerBlock, 10 * timePerBlock) to AudioEditAction.CUT,
			AudioClipConfig(0, 3 * timePerBlock) to AudioEditAction.CUT
		)
		val result = original.updateArrayViaConfigs(configs, timePerBlock)
		val expected = floatArrayOf(3f, 4f, 5f, 6f)
		assertArrayEquals(expected, result, 0.0f)
	}

	@Test
	fun `cropping the array two times`() = runTest {
		val original = FloatArray(10) { (it * 10).toFloat() }

		val configs = listOf(
			AudioClipConfig(2 * timePerBlock, 8 * timePerBlock) to AudioEditAction.CROP,
			AudioClipConfig(timePerBlock, 4 * timePerBlock) to AudioEditAction.CROP
		)

		val result = original.updateArrayViaConfigs(configs, timePerBlock)

		val expected = floatArrayOf(30f, 40f, 50f)
		assertArrayEquals(expected, result, 0.0f)
	}

	@Test
	fun `combine a mix of cut and crop 1`() = runTest {
		val original = floatArrayOf(1f, 2f, 3f, 4f, 5f)

		val configs = listOf(
			AudioClipConfig(0, 3 * timePerBlock) to AudioEditAction.CUT,
			AudioClipConfig(0, timePerBlock) to AudioEditAction.CROP,
			AudioClipConfig(0, timePerBlock) to AudioEditAction.CUT
		)
		val result = original.updateArrayViaConfigs(configs, timePerBlock)
		assertEquals(0, result.size)
	}

	@Test
	fun `combine a mix of cut and crop 2`() = runTest {
		val original = FloatArray(10) { it.toFloat() }

		val configs = listOf(
			AudioClipConfig(3 * timePerBlock, 7 * timePerBlock) to AudioEditAction.CUT,
			AudioClipConfig(2 * timePerBlock, 4 * timePerBlock) to AudioEditAction.CROP,
		)
		val result = original.updateArrayViaConfigs(configs, timePerBlock)
		val expected = floatArrayOf(2f, 7f)
		assertArrayEquals(expected, result, 0f)
	}
}
