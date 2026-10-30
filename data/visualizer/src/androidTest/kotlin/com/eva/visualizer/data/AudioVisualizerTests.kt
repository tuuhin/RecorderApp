package com.eva.visualizer.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.isGreaterThan
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isLessThan
import assertk.assertions.isLessThanOrEqualTo
import assertk.assertions.isTrue
import com.com.visualizer.data.AudioVisualizerImpl
import com.com.visualizer.data.ThreadLifecycleControllerImpl
import com.com.visualizer.domain.VisualizerState
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class AudioVisualizerTests {

    private val appContext = ApplicationProvider.getApplicationContext<Context>()
    private val testContext = InstrumentationRegistry.getInstrumentation().context
    private val lifecycleOwner = TestLifecycleOwner()

    private lateinit var visualizer: AudioVisualizerImpl

    @Before
    fun setUp() {
        visualizer = AudioVisualizerImpl(
            context = appContext,
            threadHandler = ThreadLifecycleControllerImpl("viz-test-thread"),
        )
    }

    @After
    fun tearDown() {
        visualizer.cleanUp()
    }

    @Test
    fun parsing_synthetic_audio_produces_correct_envelope() {
        val audioFile = SyntheticAACTestAudioProvider.encodeAlternatingLoudSilence(testContext)
        val audioDuration = SyntheticAACTestAudioProvider.synthetic_duration

        val (points, parseTime) = visualizationParser(audioFile, timeout = 60.seconds)

        Log.i(TAG, "PARSED $audioDuration OF AUDIO IN $parseTime WITH ${points.size} POINTS")

        val expectedPoints = (audioDuration / visualPoint).toInt()
        assertThat(points.size, "points").isGreaterThanOrEqualTo(expectedPoints - 5)
        assertThat(points.size, "points").isLessThanOrEqualTo(expectedPoints + 5)

        // the points are normalized to the [0, 1] range
        points.forEachIndexed { index, value ->
            assertThat(value, "point $index").isGreaterThanOrEqualTo(0f)
            assertThat(value, "point $index").isLessThanOrEqualTo(1f)
        }

        // the envelope alternates 5s loud and 5s silent, the checked windows
        // are well inside each segment so transitions and smoothing cannot
        // bleed into them
        val pointsPerSecond = (1.seconds / visualPoint).toInt()
        val pointsPerCycle = 10 * pointsPerSecond

        repeat(SyntheticAACTestAudioProvider.CYCLES) { cycle ->
            val cycleStart = cycle * pointsPerCycle
            val loudRange = (cycleStart + 2 * pointsPerSecond)..(cycleStart + 4 * pointsPerSecond)
            val silentRange = (cycleStart + 7 * pointsPerSecond)..(cycleStart + 9 * pointsPerSecond)

            loudRange.forEach { index ->
                assertThat(points[index], "loud window $index").isGreaterThan(0.5f)
            }
            silentRange.forEach { index ->
                assertThat(points[index], "silent window $index").isLessThan(0.2f)
            }
        }

        assertThat(parseTime, "parse time").isLessThan(maxParsingTime)
    }

    @Test
    fun parsing_audio_assets_matches_duration_and_parses_faster_than_realtime() {
        val assetNames = testContext.assets.list("").orEmpty()
            .filter { it.startsWith("sample_") && it.endsWith(".mp3") }
            .filterNotNull()
            .sorted()


        assumeTrue("no sample assets found, skipping", assetNames.isNotEmpty())

        for (assetName in assetNames) {
            val audioFile = testContext.copyAssetToCache(assetName)
            val audioDuration = audioFile.readInFileDuration()
            val completionTimeout = minOf(audioDuration, macCompletionTime)

            val (points, parseTime) = visualizationParser(audioFile, timeout = completionTimeout)

            Log.i(TAG, "PARSED $assetName ($audioDuration) IN $parseTime WITH ${points.size} POINTS")

            val expectedPoints = (audioDuration / visualPoint).toInt()
            assertThat(points.size, "$assetName points").isGreaterThanOrEqualTo(expectedPoints - 5)
            assertThat(points.size, "$assetName points").isLessThanOrEqualTo(expectedPoints + 5)

            points.forEachIndexed { index, value ->
                assertThat(value, "$assetName point $index").isGreaterThanOrEqualTo(0f)
                assertThat(value, "$assetName point $index").isLessThanOrEqualTo(1f)
            }

            // parsing must always stay faster than the length of the audio
            assertThat(parseTime, "$assetName parse time").isLessThan(audioDuration)
        }
    }

    private fun visualizationParser(audioFile: File, timeout: Duration): Pair<FloatArray, Duration> = runBlocking {
        val timed = measureTime {
            val result = visualizer.prepareVisualization(
                fileUri = Uri.fromFile(audioFile).toString(),
                lifecycleOwner = lifecycleOwner,
                timePerPointInMs = visualPoint.inWholeMilliseconds.toInt(),
            )
            assertThat(result.isSuccess, "prepareVisualization").isTrue()

            withTimeout(timeout) {
                visualizer.visualizerState.first { it == VisualizerState.FINISHED }
            }
        }
        visualizer.normalizedVisualization.value to timed
    }


    companion object {
        private const val TAG = "VISUALIZER_TEST"

        private val visualPoint = 100.milliseconds
        private val maxParsingTime = 20.seconds
        private val macCompletionTime = 5.minutes
    }
}
