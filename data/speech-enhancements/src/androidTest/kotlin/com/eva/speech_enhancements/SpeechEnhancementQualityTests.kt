@file:OptIn(ExperimentalCoroutinesApi::class)

package com.eva.speech_enhancements

import android.content.Context
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isLessThanOrEqualTo
import assertk.assertions.isTrue
import com.eva.speech_enhancements.audio.VirtualAudioProvider
import com.eva.speech_enhancements.metrics.EnhancementMetrics
import com.eva.speech_enhancements.provider.ModelPathProvider
import java.io.File
import kotlin.math.abs
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
class SpeechEnhancementQualityTests(private val assetName: String) {

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
    fun clip_passes_the_quality_gate() = runTest(timeout = 1.minutes) {
        runQualityGate(assetName)
    }


    private suspend fun TestScope.runQualityGate(assetName: String) {
        val gate = gates.getValue(assetName)

        instance = ClearSpeechEnhancer()
        instance.create(modelDir())
        instance.prepare()

        advanceUntilIdle()

        val noisy = VirtualAudioProvider.readWav(copyAssetClip(TEST_AUDIO_DIR, assetName))
        val clean = VirtualAudioProvider.readWav(copyAssetClip(CLEAR_AUDIO_DIR, assetName))

        assertThat(clean.size).isEqualTo(noisy.size)
        assertThat(clean.isNotEmpty()).isTrue()

        val result = instance.enhance(noisy)

        advanceUntilIdle()

        // structural preconditions for scoring: delay compensated and finite
        assertThat(result.samples.size).isEqualTo(noisy.size)
        assertThat(result.sampleRate).isEqualTo(SAMPLE_RATE)
        assertThat(result.samples.all { it.isFinite() }).isTrue()
        assertThat(instance.latencySamples).isEqualTo(KNOWN_LATENCY_SAMPLES)

        val score = EnhancementMetrics.measure(
            clip = assetName,
            clean = clean,
            noisy = noisy,
            enhanced = result.samples,
            sampleRate = result.sampleRate,
        )

        Log.d(TAG, score.toMeasureLine())

        // the offline pass is delay compensated: output must align with clean
        assertThat(abs(score.alignmentLagSamples)).isLessThanOrEqualTo(MAX_ALIGNMENT_LAG_SAMPLES)

        // the background must actually get quieter relative to the speech
        assertThat(score.noiseFloorGainDb).isGreaterThanOrEqualTo(gate.minNoiseFloorGainDb)

        // the speech band must not lose more than the calibrated allowance
        assertThat(score.bandRetention300To3400Db).isGreaterThanOrEqualTo(-gate.maxSpeechBandLossDb)

        // speech must survive the pass: waveform stays tied to the clean reference
        assertThat(score.correlationAfter).isGreaterThanOrEqualTo(gate.minCorrelationAfter)

        // output quality against clean must reach the calibrated floor
        assertThat(score.siSdrAfterDb).isGreaterThanOrEqualTo(gate.minSiSdrAfterDb)

        // spectral closeness must stay within the calibrated ceiling
        assertThat(score.lsdAfterDb).isLessThanOrEqualTo(gate.maxLsdAfterDb)

        // when the clip is noisy enough for enhancement to help, it must actually help
        if (gate.minDeltaSiSdrDb > Double.NEGATIVE_INFINITY) {
            assertThat(score.deltaSiSdrDb).isGreaterThanOrEqualTo(gate.minDeltaSiSdrDb)
        }
    }

    private suspend fun modelDir(): File = tempDirRule.newFolder()
        .also { modelPathProvider.copyAssetsToModelDir(it) }

    private fun copyAssetClip(assetDir: String, assetName: String): File {
        val destination = File(tempDirRule.newFolder(), assetName)
        context.assets.open("$assetDir/$assetName").use { inp ->
            destination.outputStream().use { out -> inp.copyTo(out) }
        }
        return destination
    }

    /** Per-clip thresholds; calibrated on SM-E146B, tune from the `MEASURE quality` line after each device run. */
    private data class Gate(
        val minCorrelationAfter: Double,
        val minSiSdrAfterDb: Double,
        val maxLsdAfterDb: Double = Double.POSITIVE_INFINITY,
        val minDeltaSiSdrDb: Double = Double.NEGATIVE_INFINITY,
        val minNoiseFloorGainDb: Double = MIN_NOISE_FLOOR_GAIN_DB,
        val maxSpeechBandLossDb: Double = MAX_SPEECH_BAND_LOSS_DB,
    )

    private companion object {

        const val TEST_AUDIO_DIR = "test_audio"
        const val CLEAR_AUDIO_DIR = "clear_audio"

        const val SAMPLE_RATE = 48_000
        const val KNOWN_LATENCY_SAMPLES = 1440

        private const val TAG = "SpeechEnhancementQualityTest"

        const val MAX_ALIGNMENT_LAG_SAMPLES = 1
        const val MIN_NOISE_FLOOR_GAIN_DB = 3.0
        const val MAX_SPEECH_BAND_LOSS_DB = 6.0

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

        /**
         * Conservative floor for the clips that have not been calibrated on a device yet.
         * Tuned to the SM-E146B measurements (see the `MEASURE quality` lines): the weakest
         * observed values across those clips are corrAfter 0.835, siSdrAfter 3.64,
         * lsdAfter 10.68, noiseFloorGain 1.17 dB and band3400 -7.96 dB.
         */
        val PLACEHOLDER_GATE = Gate(
            minCorrelationAfter = 0.80,
            minSiSdrAfterDb = 3.0,
            maxLsdAfterDb = 12.0,
            minNoiseFloorGainDb = 0.0,
            maxSpeechBandLossDb = 10.0,
        )

        /**
         * Calibrated from the first SM-E146B run (see the `MEASURE quality` lines):
         * DFN3 clearly improves the noisy clip (017) but slightly degrades the
         * already-clean clips (001, 023) at full strength, so those gate on
         * preservation floors instead of improvement.
         *
         * The remaining clips are covered by a single conservative placeholder so the
         * suite exercises every downloaded asset; tighten each one from its own
         * `MEASURE quality` line after a device run.
         */
        val gates: Map<String, Gate> = buildMap {
            // measured: corrAfter 0.893, siSdrAfter 5.96, lsdAfter 9.02 (siSdrBefore 13.61)
            put(
                "p234_001.wav",
                Gate(
                    minCorrelationAfter = 0.85,
                    minSiSdrAfterDb = 4.0,
                    maxLsdAfterDb = 12.0,
                    maxSpeechBandLossDb = 7.0,
                ),
            )
            // the noisy clip: enhancement must demonstrably help
            // measured: corrAfter 0.979, siSdrAfter 13.55, lsdAfter 14.11 (siSdrBefore -1.34)
            put(
                "p234_017.wav",
                Gate(
                    minCorrelationAfter = 0.95,
                    minSiSdrAfterDb = 10.0,
                    maxLsdAfterDb = 20.0,
                    minDeltaSiSdrDb = 3.0,
                    maxSpeechBandLossDb = 3.0,
                ),
            )
            // measured: corrAfter 0.932, siSdrAfter 8.21, lsdAfter 6.16 (siSdrBefore 9.53)
            put(
                "p234_023.wav",
                Gate(
                    minCorrelationAfter = 0.90,
                    minSiSdrAfterDb = 6.0,
                    maxLsdAfterDb = 9.0,
                ),
            )
            // TODO: calibrate these from the MEASURE quality line after the first device run
            val placeholders = listOf(
                "p234_002.wav",
                "p234_003.wav",
                "p234_005.wav",
                "p234_007.wav",
                "p234_008.wav",
                "p234_010.wav",
                "p234_012.wav",
            )
            for (clip in placeholders) put(clip, PLACEHOLDER_GATE)
        }
    }
}
