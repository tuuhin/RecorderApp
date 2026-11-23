@file:OptIn(ExperimentalCoroutinesApi::class)

package com.eva.speech_enhancements

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.eva.speech_enhancements.models.EnhanceOptions
import com.eva.speech_enhancements.provider.ModelPathProvider
import java.io.File
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

        val deleteOp = File(folder, CONFIG_FILE_NAME).delete()
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

    private companion object {
        const val HOP_SIZE = 480
        const val KNOWN_LATENCY_SAMPLES = 1440
        const val CONFIG_FILE_NAME = "config.ini"
    }
}
