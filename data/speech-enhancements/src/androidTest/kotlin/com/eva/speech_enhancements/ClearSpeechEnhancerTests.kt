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
import com.eva.speech_enhancements.exception.SpeechNativeException
import com.eva.speech_enhancements.models.EnhanceOptions
import com.eva.speech_enhancements.models.EnhanceOptions.Blending
import com.eva.speech_enhancements.models.EnhanceOptions.ModelTuning
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

    private lateinit var context: Context
    private lateinit var modelPathProvider: ModelPathProvider
    private lateinit var instance: IClearSpeechEnhancer

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
        instance = ClearSpeechEnhancer(modelDir())
    }

    @Test
    fun creation_reports_the_algorithmic_latency_of_the_bundled_config() = runTest {
        instance = ClearSpeechEnhancer(modelDir())

        advanceUntilIdle()
        assertThat(instance.latencySamples).isEqualTo(KNOWN_LATENCY_SAMPLES)
    }

    @Test
    fun creation_rejects_a_missing_model_directory() {
        assertFailsWith<SpeechNativeException> {
            ClearSpeechEnhancer(File(tempDirRule.root, "not-copied"))
        }
    }

    @Test
    fun creation_rejects_an_empty_model_directory() {
        assertFailsWith<IllegalArgumentException> {
            ClearSpeechEnhancer(tempDirRule.newFolder())
        }
    }

    @Test
    fun creation_rejects_a_model_directory_without_the_config_file() = runTest {
        val folder = modelDir()
        advanceUntilIdle()
        assertThat(File(folder, CONFIG_FILE_NAME.substringAfterLast('/')).delete()).isEqualTo(true)

        assertFailsWith<IllegalArgumentException> {
            ClearSpeechEnhancer(folder)
        }
    }

    @Test
    fun creation_rejects_a_model_directory_without_graphs() {
        assertFailsWith<IllegalArgumentException> {
            ClearSpeechEnhancer(tempDirRule.newFolder())
        }
    }

    @Test
    fun streaming_before_prepare_fails_fast() = runTest {
        instance = ClearSpeechEnhancer(modelDir())

        advanceUntilIdle()
        assertThat(instance.isReady).isFalse()
        assertFailsWith<SpeechEnhancerStateException> {
            instance.process(FloatArray(HOP_SIZE) { 0.1f })
        }
        assertFailsWith<SpeechEnhancerStateException> { instance.finish() }
    }

    @Test
    fun prepare_is_reported_by_isPrepared() = runTest {
        instance = ClearSpeechEnhancer(modelDir())

        instance.prepare()
        advanceUntilIdle()
        assertThat(instance.isReady).isTrue()
    }

    @Test
    fun process_accepts_an_empty_chunk_and_returns_empty() = runTest {
        instance = prepared()
        advanceUntilIdle()
        assertThat(instance.process(FloatArray(0))).isEqualTo(FloatArray(0))
    }

    @Test
    fun streaming_returns_sample_aligned_output_whose_total_matches_the_input() = runTest {
        instance = prepared()

        val input = noisySpeechLikeSignal(seconds = 1)
        val result = instance.enhanceOffline(input, chunkSize = 4096)

        // delay compensation: every input sample comes back usable and aligned
        advanceUntilIdle()
        assertThat(result.size).isEqualTo(input.size)
        assertThat(result.all { it.isFinite() }).isTrue()
        assertThat(instance.latencySamples).isEqualTo(KNOWN_LATENCY_SAMPLES)
    }

    @Test
    fun streaming_hides_the_pipeline_latency_per_chunk() = runTest {
        instance = prepared()

        // chunks shorter than the latency return nothing at first...
        val small = FloatArray(HOP_SIZE) { 0.1f }
        advanceUntilIdle()
        assertThat(instance.process(small).isEmpty()).isTrue()
        assertThat(instance.process(small).isEmpty()).isTrue()

        // ...until the pipeline has filled, then roughly the chunk size comes back
        val filled = instance.process(FloatArray(2 * KNOWN_LATENCY_SAMPLES) { 0.1f })
        assertThat(filled.isNotEmpty()).isTrue()
    }

    @Test
    fun arbitrary_chunking_produces_the_same_stream_as_a_single_chunk() = runTest {
        instance = prepared()

        val input = noisySpeechLikeSignal(seconds = 1)
        val singleChunk = instance.enhanceOffline(input, chunkSize = input.size)
        instance.reset()

        val oddChunks = instance.enhanceOffline(input, chunkSize = 477)

        advanceUntilIdle()
        assertThat(oddChunks.size).isEqualTo(singleChunk.size)
        oddChunks.indices.forEach { index ->
            assertEquals(singleChunk[index], oddChunks[index], CHUNKING_TOLERANCE)
        }
    }

    @Test
    fun process_after_finish_fails_until_reset() = runTest {
        instance = prepared()

        val input = noisySpeechLikeSignal(seconds = 1)
        instance.enhanceOffline(input)

        advanceUntilIdle()
        assertFailsWith<SpeechEnhancerStateException> {
            instance.process(FloatArray(HOP_SIZE) { 0.1f })
        }
        assertFailsWith<SpeechEnhancerStateException> { instance.finish() }

        // reset starts a fresh stream that streams normally
        instance.reset()
        val secondPass = instance.enhanceOffline(input)
        advanceUntilIdle()
        assertThat(secondPass.size).isEqualTo(input.size)
    }

    @Test
    fun reset_can_be_called_repeatedly() = runTest {
        instance = prepared()

        instance.reset()
        instance.reset()

        // still usable after the no-op resets
        val result = instance.enhanceOffline(noisySpeechLikeSignal(seconds = 1))
        advanceUntilIdle()
        assertThat(result.size).isEqualTo(SAMPLE_RATE)
    }

    @Test
    fun reset_without_a_previous_stream_is_a_no_op() = runTest {
        instance = prepared()

        instance.reset()
    }

    @Test
    fun streaming_with_zero_strength_returns_the_input() = runTest {
        instance = prepared()

        val input = noisySpeechLikeSignal(seconds = 1)
        val result =
            instance.enhanceOffline(input, options = EnhanceOptions.Custom(blending = Blending(strength = 0.0f)))

        advanceUntilIdle()
        input.indices.forEach { index ->
            assertEquals(input[index], result[index], TOLERANCE)
        }
    }

    @Test
    fun streaming_with_full_strength_changes_the_input() = runTest {
        instance = prepared()

        val input = noisySpeechLikeSignal(seconds = 1)
        val result =
            instance.enhanceOffline(input, options = EnhanceOptions.Custom(blending = Blending(strength = 1.0f)))

        val maxDifference = input
            .zip(result) { noisy, enhanced -> kotlin.math.abs(noisy - enhanced) }
            .max()

        advanceUntilIdle()
        // a full model pass on a noisy signal must move the output
        assertThat(maxDifference > 0.0f).isTrue()
    }

    @Test
    fun streaming_with_adaptive_blending_returns_a_finite_aligned_stream() = runTest {
        instance = prepared()

        val input = noisySpeechLikeSignal(seconds = 1)
        val options = EnhanceOptions.Custom(blending = Blending(strength = 1.0f, adaptive = true))
        val result = instance.enhanceOffline(input, chunkSize = 8192, options = options)

        advanceUntilIdle()
        assertThat(result.size).isEqualTo(input.size)
        assertThat(result.all { it.isFinite() }).isTrue()
    }

    @Test
    fun close_is_idempotent_and_releases_the_handle() = runTest {
        instance = prepared()

        instance.close()
        instance.close() // second release is a no-op and must not throw

        advanceUntilIdle()
        assertFailsWith<SpeechEnhancementHandleException> { instance.latencySamples }
        assertFailsWith<SpeechEnhancementHandleException> { instance.process(FloatArray(HOP_SIZE)) }
    }

    @Test
    fun default_options_request_the_full_model_output() {
        assertThat(Blending().strength).isEqualTo(1.0f)
        assertThat(Blending().adaptive).isFalse()
    }

    @Test
    fun blending_rejects_a_strength_outside_zero_to_one() {
        assertFailsWith<IllegalArgumentException> { Blending(strength = -0.01f) }
        assertFailsWith<IllegalArgumentException> { Blending(strength = 1.01f) }

        assertEquals(0.0f, Blending(strength = 0.0f).strength)
        assertEquals(0.5f, Blending(strength = 0.5f).strength)
        assertEquals(1.0f, Blending(strength = 1.0f).strength)
    }

    @Test
    fun tuning_rejects_a_negative_post_filter_beta() {
        assertFailsWith<IllegalArgumentException> { ModelTuning(postFilterBeta = -0.01f) }

        assertEquals(0.0f, ModelTuning(postFilterBeta = 0.0f).postFilterBeta)
    }

    private suspend fun prepared(): IClearSpeechEnhancer =
        ClearSpeechEnhancer(modelDir()).also { it.prepare() }

    private suspend fun modelDir(): File = tempDirRule.newFolder()
        .also { dir -> modelPathProvider.copyAssetsToModelDir(dir) }

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
        const val CHUNKING_TOLERANCE = 1e-2f
    }
}
