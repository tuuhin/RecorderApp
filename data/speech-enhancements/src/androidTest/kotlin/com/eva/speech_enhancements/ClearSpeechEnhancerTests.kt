@file:OptIn(ExperimentalCoroutinesApi::class)

package com.eva.speech_enhancements

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.eva.speech_enhancements.exception.SpeechEnhancementHandleException
import com.eva.speech_enhancements.exception.SpeechEnhancerStateException
import com.eva.speech_enhancements.models.EnhanceOptions
import com.eva.speech_enhancements.provider.ModelPathProvider
import java.io.File
import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClearSpeechEnhancerTests {

    lateinit var context: Context
    lateinit var modelPathProvider: ModelPathProvider
    lateinit var instance: IClearSpeechEnhancer

    @get:Rule
    val tempDirRule = TemporaryFolder()

    @BeforeTest
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        modelPathProvider = ModelPathProvider(context)
    }

    @AfterTest
    fun cleanUp() {
        if (::instance.isInitialized) instance.close()
    }


    @Test
    fun plain_run_test_to_check_if_the_jni_connection_is_correct() = runTest {
        val folder = tempDirRule.newFolder()
        modelPathProvider.copyAssetsToModelDir(folder)

        instance = ClearSpeechEnhancer()
        instance.create(folder)
    }

    @Test
    fun create_reports_the_algorithmic_latency_of_the_bundled_config() = runTest {
        instance = ClearSpeechEnhancer()
        instance.create(modelDir())

        advanceUntilIdle()
        assertThat(instance.latencySamples).isEqualTo(KNOWN_LATENCY_SAMPLES)
    }

    @Test
    fun create_rejects_a_missing_model_directory() {
        instance = ClearSpeechEnhancer()

        assertFailsWith<IllegalStateException> {
            instance.create(File(tempDirRule.root, "not-copied"))
        }
    }

    @Test
    fun create_rejects_an_empty_model_directory() {
        instance = ClearSpeechEnhancer()

        assertFailsWith<IllegalArgumentException> { instance.create(tempDirRule.newFolder()) }
    }

    @Test
    fun create_rejects_a_model_directory_without_the_config_file() = runTest {
        val folder = modelDir()

        advanceUntilIdle()

        val deleteOp = File(folder, CONFIG_FILE_NAME.substringAfterLast('/')).delete()
        assertThat(deleteOp).isEqualTo(true)

        instance = ClearSpeechEnhancer()
        assertFailsWith<IllegalArgumentException> { instance.create(folder) }
    }

    @Test
    fun create_rejects_a_model_directory_without_graphs() {
        val folder = tempDirRule.newFolder()

        instance = ClearSpeechEnhancer()
        assertFailsWith<IllegalArgumentException> { instance.create(folder) }
    }

    @Test
    fun create_twice_is_rejected_and_keeps_the_first_instance_usable() = runTest {
        instance = ClearSpeechEnhancer()
        instance.create(modelDir())

        advanceUntilIdle()
        assertFailsWith<IllegalStateException> { instance.create(modelDir()) }

        assertThat(instance.latencySamples).isEqualTo(KNOWN_LATENCY_SAMPLES)
    }

    @Test
    fun calls_before_create_fail_fast() = runTest {
        instance = ClearSpeechEnhancer()

        advanceUntilIdle()

        assertFailsWith<IllegalStateException> { instance.latencySamples }
        assertFailsWith<IllegalStateException> { instance.prepare() }
        assertFailsWith<IllegalStateException> { instance.enhance(FloatArray(HOP_SIZE) { 0.1f }) }
    }

    @Test
    fun enhance_rejects_empty_samples() = runTest {
        instance = ClearSpeechEnhancer()
        advanceUntilIdle()

        assertFailsWith<IllegalArgumentException> { instance.enhance(FloatArray(0)) }
    }

    @Test
    fun enhance_rejects_sample_rates_other_than_48_khz() = runTest {
        instance = ClearSpeechEnhancer()

        advanceUntilIdle()
        assertFailsWith<IllegalArgumentException> {
            instance.enhance(FloatArray(HOP_SIZE), sampleRate = 16_000)
        }
    }

    @Test
    fun enhance_produces_a_delay_compensated_finite_result() = runTest {
        instance = ClearSpeechEnhancer()
        instance.create(modelDir())
        instance.prepare()

        advanceUntilIdle()

        val input = noisySpeechLikeSignal(seconds = 1)
        val result = instance.enhance(input)

        advanceUntilIdle()

        // delay compensation: the offline pass drops the algorithmic latency,
        // so every input sample comes back usable
        assertThat(result.samples.size).isEqualTo(input.size)
        assertThat(result.sampleRate).isEqualTo(48_000)
        assertThat(result.isEmpty).isFalse()
        assertThat(result.samples.all { it.isFinite() }).isTrue()
        assertThat(instance.latencySamples).isEqualTo(KNOWN_LATENCY_SAMPLES)
        assertThat(result.realtimeFactor > 0.0).isTrue()
    }

    @Test
    fun enhance_works_without_an_explicit_prepare() = runTest {
        instance = ClearSpeechEnhancer()
        instance.create(modelDir())

        advanceUntilIdle()

        val input = noisySpeechLikeSignal(seconds = 1)
        val result = instance.enhance(input)

        advanceUntilIdle()

        assertThat(result.samples.size).isEqualTo(input.size)
    }

    @Test
    fun enhance_with_zero_strength_returns_the_input() = runTest {
        instance = ClearSpeechEnhancer()
        instance.create(modelDir())
        instance.prepare()

        advanceUntilIdle()

        val input = noisySpeechLikeSignal(seconds = 1)
        val result = instance.enhance(input, options = EnhanceOptions(strength = 0.0f))

        advanceUntilIdle()

        input.indices.forEach { index ->
            assertEquals(input[index], result.samples[index], TOLERANCE)
        }
    }

    @Test
    fun enhance_with_full_strength_changes_the_input() = runTest {
        instance = ClearSpeechEnhancer()
        instance.create(modelDir())
        instance.prepare()

        advanceUntilIdle()

        val input = noisySpeechLikeSignal(seconds = 1)
        val result = instance.enhance(input, options = EnhanceOptions(strength = 1.0f))

        advanceUntilIdle()

        val maxDifference = input
            .zip(result.samples) { noisy, enhanced -> kotlin.math.abs(noisy - enhanced) }
            .max()

        // a full model pass on a noisy signal must move the output
        assertThat(maxDifference > 0.0f).isTrue()
    }

    @Test
    fun enhance_twice_without_reset_fails_fast() = runTest {
        instance = ClearSpeechEnhancer()
        instance.create(modelDir())
        instance.prepare()

        advanceUntilIdle()

        val input = noisySpeechLikeSignal(seconds = 1)
        instance.enhance(input)

        advanceUntilIdle()

        assertFailsWith<SpeechEnhancerStateException> { instance.enhance(input) }
    }

    @Test
    fun enhance_after_reset_matches_a_fresh_instance() = runTest {
        instance = ClearSpeechEnhancer()
        instance.create(modelDir())
        instance.prepare()

        val input = noisySpeechLikeSignal(seconds = 1)

        advanceUntilIdle()

        instance.enhance(input)
        assertThat(instance.reset()).isTrue()
        val afterReset = instance.enhance(input)

        val fresh = ClearSpeechEnhancer()
        fresh.create(modelDir())
        fresh.prepare()
        val fromFresh = fresh.enhance(input)
        fresh.close()

        advanceUntilIdle()

        assertEquals(afterReset.samples.size, fromFresh.samples.size)
        // ONNX Runtime is not bit-exact across runs; compare within a tolerance
        afterReset.samples.indices.forEach { index ->
            assertEquals(
                fromFresh.samples[index],
                afterReset.samples[index],
                RESET_COMPARISON_TOLERANCE,
            )
        }
    }

    @Test
    fun reset_can_be_called_repeatedly() = runTest {
        instance = ClearSpeechEnhancer()
        instance.create(modelDir())
        instance.prepare()

        advanceUntilIdle()

        val input = noisySpeechLikeSignal(seconds = 1)
        instance.enhance(input)

        advanceUntilIdle()

        // the first reset clears the consumed stream state
        assertThat(instance.reset()).isTrue()
        // the second is a no-op: nothing left to reset
        assertThat(instance.reset()).isFalse()

        // still usable after the no-op second reset
        val result = instance.enhance(input)

        advanceUntilIdle()

        assertThat(result.samples.size).isEqualTo(input.size)
    }

    @Test
    fun reset_without_a_previous_enhance_reports_nothing_to_reset() = runTest {
        instance = ClearSpeechEnhancer()
        instance.create(modelDir())
        instance.prepare()

        advanceUntilIdle()

        assertThat(instance.reset()).isFalse()
    }

    @Test
    fun reset_before_create_fails_fast() = runTest {
        instance = ClearSpeechEnhancer()

        advanceUntilIdle()

        assertFailsWith<SpeechEnhancementHandleException> { instance.reset() }
    }

    @Test
    fun close_is_idempotent_and_releases_the_handle() = runTest {
        instance = ClearSpeechEnhancer()
        instance.create(modelDir())

        instance.close()
        instance.close() // second release is a no-op and must not throw

        advanceUntilIdle()
        assertFailsWith<IllegalStateException> { instance.latencySamples }
    }

    @Test
    fun default_options_request_the_full_model_output() {
        assertEquals(1.0f, EnhanceOptions().strength)
    }

    @Test
    fun options_reject_a_strength_outside_zero_to_one() {
        assertFailsWith<IllegalArgumentException> { EnhanceOptions(strength = -0.01f) }
        assertFailsWith<IllegalArgumentException> { EnhanceOptions(strength = 1.01f) }

        assertEquals(0.0f, EnhanceOptions(strength = 0.0f).strength)
        assertEquals(0.5f, EnhanceOptions(strength = 0.5f).strength)
        assertEquals(1.0f, EnhanceOptions(strength = 1.0f).strength)
    }

    private suspend fun modelDir(): File = tempDirRule.newFolder()
        .also { modelPathProvider.copyAssetsToModelDir(it) }

    /**
     * A 48 kHz mono signal shaped roughly like speech: a few voiced formant tones with
     * an amplitude envelope, plus stationary white noise for the model to suppress.
     */
    private fun noisySpeechLikeSignal(seconds: Int = 0): FloatArray {
        val sampleCount = SAMPLE_RATE * seconds
        val samples = FloatArray(sampleCount)

        for (index in 0 until sampleCount) {
            val t = index.toDouble()
            // three formant-ish tones with a slow amplitude envelope
            val speech = 0.30 * sin(2.0 * PI * 220.0 * t / SAMPLE_RATE) +
                0.20 * sin(2.0 * PI * 880.0 * t / SAMPLE_RATE) +
                0.10 * sin(2.0 * PI * 2400.0 * t / SAMPLE_RATE)
            val envelope = 0.5 * (1.0 + sin(2.0 * PI * 0.5 * t / SAMPLE_RATE))
            // deterministic white-ish noise in [-0.05, 0.05]
            val noise = NOISE_AMPLITUDE * (2.0 * deterministicNoise(index) - 1.0)

            samples[index] = (speech * envelope + noise).toFloat().coerceIn(-1.0f, 1.0f)
        }
        return samples
    }

    /** A small deterministic hash used instead of Random for reproducible noise. */
    private fun deterministicNoise(index: Int): Double {
        var value = (index + 1) * 2654435761L
        value = value xor (value ushr 13)
        value *= 1_547_858_496_639_878_619L
        value = value xor (value ushr 16)
        return ((value % 10_007L) / 10_007.0).coerceIn(0.0, 1.0)
    }

    private companion object {
        const val SAMPLE_RATE = 48_000
        const val HOP_SIZE = 480
        const val KNOWN_LATENCY_SAMPLES = 1440
        const val CONFIG_FILE_NAME = "model/config.ini"
        const val NOISE_AMPLITUDE = 0.05

        // float rounding through the native layer
        const val TOLERANCE = 1e-4f

        // ONNX Runtime thread scheduling is not bit-exact across runs
        const val RESET_COMPARISON_TOLERANCE = 1e-2f
    }
}
