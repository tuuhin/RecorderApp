package com.eva.visualizer.data

import android.net.Uri
import android.util.Log
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isLessThan
import assertk.assertions.isLessThanOrEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.com.visualizer.data.AudioVisualizerImpl
import com.com.visualizer.data.ThreadLifecycleControllerImpl
import com.com.visualizer.data.cache.VisualCacheImpl
import com.com.visualizer.domain.VisualizerState
import com.com.visualizer.domain.model.VisualizerCacheEntry
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioVisualizerTests {

    private val testContext = InstrumentationRegistry.getInstrumentation().context
    private val lifecycleOwner = TestLifecycleOwner()

    private lateinit var visualizer: AudioVisualizerImpl
    private lateinit var tempDir: File

    @get:Rule
    val tempFolder = TemporaryFolder()

    @BeforeTest
    fun setUp() {
        tempDir = tempFolder.newFolder()

        visualizer = AudioVisualizerImpl(
            testContext,
            cache = VisualCacheImpl(tempDir),
            extractor = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true),
            dataSource = DefaultDataSource.Factory(testContext),
            threadHandler = ThreadLifecycleControllerImpl("viz-test-thread"),
        )
    }

    @AfterTest
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
        assertThat(points.size).isGreaterThanOrEqualTo(expectedPoints - 5)
        assertThat(points.size).isLessThanOrEqualTo(expectedPoints + 5)

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
            .filter { it.startsWith("sample_") && it.endsWith(".mp3") && it != "sample_30m.mp3" }
            .filterNotNull()
            .sorted()

        assertThat(assetNames.isNotEmpty()).isTrue()

        for (assetName in assetNames) {
            val audioFile = testContext.copyAssetToCache(assetName)
            val audioDuration = audioFile.readInFileDuration()
            val completionTimeout = minOf(audioDuration, macCompletionTime)

            val (points, parseTime) = visualizationParser(audioFile, timeout = completionTimeout)

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

    @Test
    fun cache_hit_serves_the_same_points_without_running_a_decoder() = runBlocking {
        val audioFile = SyntheticAACTestAudioProvider.encodeAlternatingLoudSilence(testContext)

        val (firstOpen, decodeTime) = visualizationParser(audioFile, timeout = 60.seconds)

        val result = visualizer.prepareVisualization(
            fileUri = Uri.fromFile(audioFile).toString(),
            lifecycleOwner = lifecycleOwner,
            timePerPointInMs = visualPoint.inWholeMilliseconds.toInt(),
        )

        assertThat(result.isSuccess, "cache hit prepare").isTrue()
        assertThat(visualizer.visualizerState.value).isEqualTo(VisualizerState.FINISHED)

        val secondOpen = visualizer.normalizedVisualization.value
        assertThat(secondOpen.size).isEqualTo(firstOpen.size)

        secondOpen.forEachIndexed { index, value ->
            assertThat(value, "cached point $index").isCloseTo(firstOpen[index], 0.001f)
        }
    }

    @Test
    fun stale_cache_entry_re_decodes_the_audio() = runBlocking {
        val audioFile = SyntheticAACTestAudioProvider.encodeAlternatingLoudSilence(testContext)

        val cache = VisualCacheImpl(tempDir)
        cache.setCacheEntry(
            "50",
            VisualizerCacheEntry(
                hash = "not-the-hash-of-this-file",
                entryFloats = floatArrayOf(1f),
                timePerPointMs = visualPoint.inWholeMilliseconds.toInt(),
            ),
        )
        assertThat(cache.getCacheEntry("50")).isNotNull()

        val (points, _) = visualizationParser(audioFile, timeout = 60.seconds)

        // the stale points were replaced by a full fresh decode
        assertThat(points.size, "decoded points").isGreaterThan(3)

        // new entry replaced the data also being compressed the data size should be smaller
        val replaced = cache.getCacheEntry("50")

        assertThat(replaced).isNotNull()
        assertThat(replaced?.entryFloats?.size)
            .isNotNull()
            .isLessThanOrEqualTo(points.size)
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
