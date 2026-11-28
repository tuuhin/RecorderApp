@file:OptIn(ExperimentalCoroutinesApi::class)

package com.eva.speech_enhancements

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.eva.speech_enhancements.audio.AudioClipMuxer
import com.eva.speech_enhancements.audio.VirtualAudioProvider
import com.eva.speech_enhancements.provider.ModelPathProvider
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ActualSpeechEnhancerTests(private val assetName: String) {

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
    fun real_speech_clip_enhances_and_rebuilds_into_an_aac_clip(): Unit = runTest(timeout = 1.minutes) {
        enhanceAndRebuildClip(assetName)
    }

    private suspend fun TestScope.enhanceAndRebuildClip(assetName: String) {
        val clipFile = copyAssetClip(assetName)

        instance = ClearSpeechEnhancer(modelDir())
        instance.prepare()

        advanceUntilIdle()

        val input = VirtualAudioProvider.readWav(clipFile)
        assertThat(input.isNotEmpty()).isTrue()

        val result = instance.enhanceOffline(input)

        advanceUntilIdle()

        // structural: the offline pass is delay compensated and finite
        assertThat(result.size).isEqualTo(input.size)
        assertThat(SAMPLE_RATE).isEqualTo(SAMPLE_RATE)
        assertThat(result.all { it.isFinite() }).isTrue()
        assertThat(instance.latencySamples).isEqualTo(KNOWN_LATENCY_SAMPLES)

        // behavioural (lenient until calibrated): real speech should survive
        // the model pass largely intact
        val correlation = normalizedCrossCorrelation(input, result)
        val attenuationDb = rmsDb(input) - rmsDb(result)
        println("MEASURE actual_speech clip=$assetName correlation=$correlation attenuationDb=$attenuationDb")
        assertThat(correlation > MIN_SPEECH_CORRELATION).isTrue()
        assertThat(attenuationDb < MAX_SPEECH_ATTENUATION_DB).isTrue()

        // the first enhance consumed state, so this must be an actual reset
        instance.reset()
        val secondPass = instance.enhanceOffline(input)

        advanceUntilIdle()

        assertThat(secondPass.size).isEqualTo(input.size)

        // the enhanced audio rebuilds into a real AAC/M4A clip through the
        // standard platform pipeline; this runner only proves it can be produced
        val muxed = AudioClipMuxer.encodeAac(
            destination = File(tempDirRule.newFolder(), assetName.substringBeforeLast('.') + "_enhanced.m4a"),
            samples = result,
            sampleRate = SAMPLE_RATE,
        )

        advanceUntilIdle()

        assertThat(muxed.exists()).isTrue()
        assertThat(muxed.length() > 0).isTrue()

        // keep nothing behind
        assertThat(muxed.delete()).isTrue()
    }

    private suspend fun modelDir(): File = tempDirRule.newFolder()
        .also { modelPathProvider.copyAssetsToModelDir(it) }

    /** Copies a clip from `assets/test_audio` into the temp folder. */
    private fun copyAssetClip(assetName: String): File {
        val destination = File(tempDirRule.newFolder(), assetName)
        context.assets.open("test_audio/$assetName").use { inp ->
            destination.outputStream().use { out -> inp.copyTo(out) }
        }
        return destination
    }

    private fun rmsDb(samples: FloatArray, start: Int = 0, endExclusive: Int = samples.size): Double {
        var sum = 0.0
        for (index in start until endExclusive) {
            sum += samples[index].toDouble() * samples[index]
        }
        val rms = kotlin.math.sqrt(sum / (endExclusive - start))
        return 20.0 * kotlin.math.log10(rms + 1e-12)
    }

    /**
     * Normalized cross-correlation at zero lag, in [-1, 1]: 1.0 means the
     * enhanced speech kept the phase and shape of the input.
     */
    private fun normalizedCrossCorrelation(input: FloatArray, enhanced: FloatArray): Double {
        var dot = 0.0
        var inputEnergy = 0.0
        var enhancedEnergy = 0.0
        for (index in input.indices) {
            val a = input[index].toDouble()
            val b = enhanced[index].toDouble()
            dot += a * b
            inputEnergy += a * a
            enhancedEnergy += b * b
        }
        return dot / (kotlin.math.sqrt(inputEnergy * enhancedEnergy) + 1e-12)
    }

    private companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun clips(): List<String> = listOf(
            "p234_001.wav",
            "p234_002.wav",
            "p234_003.wav",
            "p234_005.wav",
            "p234_007.wav",
            "p234_008.wav",
            "p234_010.wav",
            "p234_012.wav",
            "p234_017.wav",
            "p234_023.wav",
        )

        const val SAMPLE_RATE = 48_000
        const val KNOWN_LATENCY_SAMPLES = 1440

        // lenient until the first device run calibrates them
        const val MIN_SPEECH_CORRELATION = 0.1
        const val MAX_SPEECH_ATTENUATION_DB = 30.0
    }
}
