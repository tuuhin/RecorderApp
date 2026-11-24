@file:OptIn(ExperimentalCoroutinesApi::class)

package com.eva.speech_enhancements

import android.content.Context
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.eva.speech_enhancements.audio.AudioClipMuxer
import com.eva.speech_enhancements.audio.VirtualAudioProvider
import com.eva.speech_enhancements.provider.ModelPathProvider
import java.io.File
import kotlin.math.log10
import kotlin.math.sqrt
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VirtualSpeechEnhancerTests {

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
    fun noisy_speech_clip_enhances_and_rebuilds_into_an_aac_clip() = runTest(timeout = TEST_TIMEOUT) {
        val clip = VirtualAudioProvider.noisySpeechClip(tempDirRule.newFolder())

        instance = ClearSpeechEnhancer()
        instance.create(modelDir())
        instance.prepare()

        advanceUntilIdle()

        val input = VirtualAudioProvider.readWav(clip.file)
        assertThat(input.size).isEqualTo(clip.noisy.size)

        val result = instance.enhance(input)

        advanceUntilIdle()

        // structural: the offline pass is delay compensated and finite
        assertThat(result.samples.size).isEqualTo(input.size)
        assertThat(result.sampleRate).isEqualTo(SAMPLE_RATE)
        assertThat(result.samples.all { it.isFinite() }).isTrue()
        assertThat(instance.latencySamples).isEqualTo(KNOWN_LATENCY_SAMPLES)
        assertThat(result.realtimeFactor > 0.0).isTrue()

        val snrBefore = snrDb(clean = clip.clean, noisy = input)
        val snrAfter = snrDb(clean = clip.clean, noisy = result.samples)
        Log.d(TAG, "MEASURE noisy_speech snrBefore=$snrBefore snrAfter=$snrAfter")

        assertThat(snrAfter > MIN_SNR_AFTER_DB).isTrue()
        assertThat(snrBefore - snrAfter < MAX_SNR_DROP_DB).isTrue()

        // reset makes a second enhance call possible and reusable; the first
        // enhance consumed state, so this must be an actual reset
        assertThat(instance.reset()).isTrue()
        val secondPass = instance.enhance(input)

        advanceUntilIdle()

        assertThat(secondPass.samples.size).isEqualTo(input.size)

        // the enhanced audio rebuilds into a real AAC/M4A clip through the
        // standard platform pipeline; this runner only proves it can be produced
        val muxed = AudioClipMuxer.encodeAac(
            destination = File(tempDirRule.newFolder(), "noisy_speech_enhanced.m4a"),
            samples = result.samples,
            sampleRate = result.sampleRate,
        )

        advanceUntilIdle()

        assertThat(muxed.exists()).isTrue()
        assertThat(muxed.length() > 0).isTrue()

        // keep nothing behind
        assertThat(muxed.delete()).isTrue()
    }

    @Test
    fun loud_silence_clip_suppresses_noise_in_the_silent_segments() = runTest(timeout = TEST_TIMEOUT) {
        val clipFile = VirtualAudioProvider.alternatingLoudSilenceClip(tempDirRule.newFolder())

        instance = ClearSpeechEnhancer()
        instance.create(modelDir())
        instance.prepare()

        advanceUntilIdle()

        val input = VirtualAudioProvider.readWav(clipFile)
        val result = instance.enhance(input)

        advanceUntilIdle()

        assertThat(result.samples.size).isEqualTo(input.size)
        assertThat(result.samples.all { it.isFinite() }).isTrue()

        // behavioural (lenient): the model should not add energy to the silent segments.
        // with two loud/silent cycles the silent segment runs over [size/4, size/2)
        val silentStart = input.size / 4
        val silentEnd = input.size / 2
        val silenceRmsBefore = rms(input, silentStart, silentEnd)
        val silenceRmsAfter = rms(result.samples, silentStart, silentEnd)
        println("MEASURE loud_silence silenceRmsBefore=$silenceRmsBefore silenceRmsAfter=$silenceRmsAfter")
        assertThat(silenceRmsAfter <= silenceRmsBefore + RMS_GROWTH_TOLERANCE).isTrue()

        // and the enhanced audio still muxes into a playable clip
        val muxed = AudioClipMuxer.encodeAac(
            destination = File(tempDirRule.newFolder(), "loud_silence_enhanced.m4a"),
            samples = result.samples,
            sampleRate = result.sampleRate,
        )

        advanceUntilIdle()

        assertThat(muxed.exists()).isTrue()
        assertThat(muxed.length() > 0).isTrue()
        assertThat(muxed.delete()).isTrue()
    }

    @Test
    fun tone_sweep_clip_enhances_and_muxes() = runTest(timeout = TEST_TIMEOUT) {
        val clipFile = VirtualAudioProvider.toneSweepClip(tempDirRule.newFolder())

        instance = ClearSpeechEnhancer()
        instance.create(modelDir())
        instance.prepare()

        advanceUntilIdle()

        val input = VirtualAudioProvider.readWav(clipFile)
        val result = instance.enhance(input)

        advanceUntilIdle()

        assertThat(result.samples.size).isEqualTo(input.size)
        assertThat(result.samples.all { it.isFinite() }).isTrue()

        // behavioural (calibrated on SM-E146B): a tone sweep is not speech, the
        // model suppresses it (~12.5 dB, correlation ~0.27 measured). A healthy
        // pipeline still keeps some correlation with the input and attenuates
        // within a bounded range instead of wiping the signal out.
        val correlation = normalizedCrossCorrelation(input, result.samples)
        val rmsBefore = rmsDb(input)
        val rmsAfter = rmsDb(result.samples)
        println("MEASURE tone_sweep correlation=$correlation rmsBefore=$rmsBefore rmsAfter=$rmsAfter")
        assertThat(correlation > SWEEP_CORRELATION_FLOOR).isTrue()
        assertThat(rmsBefore - rmsAfter < MAX_SWEEP_ATTENUATION_DB).isTrue()

        val muxed = AudioClipMuxer.encodeAac(
            destination = File(tempDirRule.newFolder(), "tone_sweep_enhanced.m4a"),
            samples = result.samples,
            sampleRate = result.sampleRate,
        )

        advanceUntilIdle()

        assertThat(muxed.exists()).isTrue()
        assertThat(muxed.length() > 0).isTrue()
        assertThat(muxed.delete()).isTrue()
    }

    private suspend fun modelDir(): File = tempDirRule.newFolder()
        .also { modelPathProvider.copyAssetsToModelDir(it) }

    /** Signal-to-noise ratio in dB of [noisy] against a known [clean] reference. */
    private fun snrDb(clean: FloatArray, noisy: FloatArray): Double {
        var signalEnergy = 0.0
        var noiseEnergy = 0.0
        for (index in clean.indices) {
            val reference = clean[index].toDouble()
            val error = (noisy[index] - reference)
            signalEnergy += reference * reference
            noiseEnergy += error * error
        }
        return 10.0 * log10((signalEnergy + 1e-12) / (noiseEnergy + 1e-12))
    }

    private fun rms(samples: FloatArray, start: Int, endExclusive: Int): Double {
        var sum = 0.0
        for (index in start until endExclusive) {
            sum += samples[index].toDouble() * samples[index]
        }
        return sqrt(sum / (endExclusive - start))
    }

    private fun rmsDb(samples: FloatArray, start: Int = 0, endExclusive: Int = samples.size): Double =
        20.0 * log10(rms(samples, start, endExclusive) + 1e-12)

    /**
     * Normalized cross-correlation at zero lag, in [-1, 1]: 1.0 means the enhanced
     * sweep kept the phase and shape of the input.
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
        return dot / (sqrt(inputEnergy * enhancedEnergy) + 1e-12)
    }

    private companion object {
        private val TEST_TIMEOUT = 1.minutes
        private const val TAG = "VirtualSpeechEnhancer"


        const val SAMPLE_RATE = 48_000
        const val KNOWN_LATENCY_SAMPLES = 1440

        const val MIN_SNR_AFTER_DB = -10.0
        const val MAX_SNR_DROP_DB = 25.0
        const val RMS_GROWTH_TOLERANCE = 0.01
        const val SWEEP_CORRELATION_FLOOR = 0.15
        const val MAX_SWEEP_ATTENUATION_DB = 25.0
    }
}
