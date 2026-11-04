package com.eva.visualizer.data

import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.media3.common.util.UnstableApi
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
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.com.visualizer.data.AudioVisualizerImpl
import com.com.visualizer.data.MediaCodecPCMDataDecoder
import com.com.visualizer.data.ThreadLifecycleControllerImpl
import com.com.visualizer.data.cache.FileHashGenerator
import com.com.visualizer.data.cache.VisualCacheImpl
import com.com.visualizer.domain.ThreadController
import com.com.visualizer.domain.VisualizerCache
import com.com.visualizer.domain.VisualizerSession
import com.com.visualizer.domain.VisualizerState
import com.com.visualizer.domain.model.VisualizerCacheEntry
import java.io.File
import java.util.Collections
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Before
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
    private lateinit var currentOwner: VisualizerSession
    private var nextPlayerId = 0L

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Before
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

    @After
    fun tearDown() = runBlocking {
        if (::currentOwner.isInitialized) visualizer.cleanUpAndAwait(currentOwner)
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
    fun parsing_synthetic_aac_matches_duration_and_parses_faster_than_realtime() {
        val audioFile = SyntheticAACTestAudioProvider.encodeAlternatingLoudSilence(testContext)
        val audioDuration = audioFile.readInFileDuration()

        val (points, parseTime) = visualizationParser(audioFile, timeout = minOf(audioDuration, macCompletionTime))

        val expectedPoints = (audioDuration / visualPoint).toInt()
        assertThat(points.size, "synthetic AAC points").isGreaterThanOrEqualTo(expectedPoints - 5)
        assertThat(points.size, "synthetic AAC points").isLessThanOrEqualTo(expectedPoints + 5)

        points.forEachIndexed { index, value ->
            assertThat(value, "synthetic AAC point $index").isGreaterThanOrEqualTo(0f)
            assertThat(value, "synthetic AAC point $index").isLessThanOrEqualTo(1f)
        }

        assertThat(parseTime, "synthetic AAC parse time").isLessThan(audioDuration)
    }

    @Test
    fun givenLongAudioWhenDecodedThenSnapshotsGrowProgressively() = runBlocking {
        val audioFile = SyntheticAACTestAudioProvider.encodeAlternatingLoudSilence(testContext)
        currentOwner = newOwner()
        val sizes = mutableListOf<Int>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            visualizer.normalizedVisualization(currentOwner).collect { values ->
                if (values.isNotEmpty()) sizes.add(values.size)
            }
        }

        try {
            val result = visualizer.prepareVisualization(
                Uri.fromFile(audioFile).toString(),
                lifecycleOwner,
                currentOwner,
                visualPoint.inWholeMilliseconds.toInt(),
            )
            assertThat(result.isSuccess).isTrue()
            withTimeout(60.seconds) {
                visualizer.visualizerState(currentOwner).first { it == VisualizerState.FINISHED }
            }
            yield()
            assertThat(sizes.size).isGreaterThan(1)
            assertThat(sizes.zipWithNext().all { (first, next) -> next > first }).isTrue()
            assertThat(sizes.last()).isGreaterThanOrEqualTo((SyntheticAACTestAudioProvider.synthetic_duration / visualPoint).toInt() - 5)
        } finally {
            collector.cancelAndJoin()
        }
    }

    @Test
    fun parsing_audio_assets_matches_duration_and_parses_faster_than_realtime() {
        val assetNames = testContext.assets.list("").orEmpty()
            .filter { it.startsWith("sample_") && it.endsWith(".mp3") && it != "sample_30m.mp3" }
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
            assertThat(parseTime, "$assetName parse time").isLessThan(audioDuration)
        }
    }

    @Test
    fun cache_hit_serves_the_same_points_without_running_a_decoder() = runBlocking {
        val audioFile = SyntheticAACTestAudioProvider.encodeAlternatingLoudSilence(testContext)

        val (firstOpen, decodeTime) = visualizationParser(audioFile, timeout = 60.seconds)
        val firstOwner = currentOwner
        val secondOwner = newOwner()

        val result = visualizer.prepareVisualization(
            fileUri = Uri.fromFile(audioFile).toString(),
            lifecycleOwner = lifecycleOwner,
            session = secondOwner,
            timePerPointInMs = visualPoint.inWholeMilliseconds.toInt(),
        )

        assertThat(result.isSuccess, "cache hit prepare").isTrue()
        currentOwner = secondOwner
        assertThat(visualizer.visualizerState(secondOwner).first()).isEqualTo(VisualizerState.FINISHED)

        val secondOpen = visualizer.normalizedVisualization(secondOwner).first()
        assertThat(secondOpen.size).isEqualTo(firstOpen.size)
        assertThat(visualizer.visualizerState(firstOwner).first()).isEqualTo(VisualizerState.NOT_STARTED)

        secondOpen.forEachIndexed { index, value ->
            assertThat(value, "cached point $index").isCloseTo(firstOpen[index], 0.001f)
        }
    }

    @Test
    fun givenMatchingHashWhenPointDurationChangesThenCachedWindowsAreReused() = runBlocking {
        val audioFile = SyntheticAACTestAudioProvider.encodeAlternatingLoudSilence(testContext)
        val uri = Uri.fromFile(audioFile)
        val hash = checkNotNull(FileHashGenerator(testContext).generateHash(uri.toString()))
        val cache = VisualCacheImpl(tempDir)
        cache.setCacheEntry(uri.toString(), VisualizerCacheEntry(hash, floatArrayOf(1f, 0f, 0f, 1f), 100))
        val noDecoder = object : ThreadController {
            override fun bindToLifecycle(lifecycleOwner: LifecycleOwner): Handler =
                error("A matching cache entry must not start a decoder")

            override fun stopThread(handler: Handler?, maxWaitTime: Long) = Unit
        }
        visualizer = AudioVisualizerImpl(
            context = testContext,
            cache = cache,
            extractor = DefaultExtractorsFactory(),
            dataSource = DefaultDataSource.Factory(testContext),
            threadHandler = noDecoder,
        )
        currentOwner = newOwner()

        val coarse = visualizer.prepareVisualization(uri.toString(), lifecycleOwner, currentOwner, 200)
        assertThat(coarse.isSuccess).isTrue()
        val coarsePoints = visualizer.normalizedVisualization(currentOwner).first()
        assertThat(coarsePoints.size).isEqualTo(2)
        coarsePoints.forEach { assertThat(it).isCloseTo(kotlin.math.sqrt(0.5f), 0.001f) }

        val fine = visualizer.prepareVisualization(uri.toString(), lifecycleOwner, currentOwner, 50)
        assertThat(fine.isSuccess).isTrue()
        val finePoints = visualizer.normalizedVisualization(currentOwner).first()
        assertThat(finePoints.size).isEqualTo(8)
        assertThat(finePoints.first()).isCloseTo(1f, 0.001f)
        assertThat(finePoints[3]).isCloseTo(0f, 0.001f)
        assertThat(finePoints.last()).isCloseTo(1f, 0.001f)

        val invalid = visualizer.prepareVisualization(uri.toString(), lifecycleOwner, currentOwner, 0)
        assertThat(invalid.isFailure).isTrue()
        assertThat(visualizer.visualizerState(currentOwner).first()).isEqualTo(VisualizerState.FINISHED)
        assertThat(cache.getCacheEntry(uri.toString())?.timePerPointMs).isEqualTo(100)
    }

    @Test
    fun givenChangedAudioHashWhenSameOwnerReopensThenAudioIsDecodedAgain() = runBlocking {
        val original = SyntheticAACTestAudioProvider.encodeAlternatingLoudSilence(testContext)
        val changedFile = tempFolder.newFile("changed-audio.m4a")
        original.copyTo(changedFile, overwrite = true)
        val uri = Uri.fromFile(changedFile)
        val cache = VisualCacheImpl(tempDir)
        var bindings = 0
        val controller = object : ThreadController {
            val delegate = ThreadLifecycleControllerImpl("changed-hash-thread")

            override fun bindToLifecycle(lifecycleOwner: LifecycleOwner): Handler? {
                bindings++
                return delegate.bindToLifecycle(lifecycleOwner)
            }

            override fun stopThread(handler: Handler?, maxWaitTime: Long) = delegate.stopThread(handler, maxWaitTime)
        }
        visualizer = AudioVisualizerImpl(
            context = testContext,
            cache = cache,
            extractor = DefaultExtractorsFactory(),
            dataSource = DefaultDataSource.Factory(testContext),
            threadHandler = controller,
        )
        currentOwner = newOwner()
        val duration = visualPoint.inWholeMilliseconds.toInt()
        assertThat(
            visualizer.prepareVisualization(
                uri.toString(),
                lifecycleOwner,
                currentOwner,
                duration,
            ).isSuccess,
        ).isTrue()
        withTimeout(60.seconds) { visualizer.visualizerState(currentOwner).first { it == VisualizerState.FINISHED } }
        val originalHash = checkNotNull(cache.getCacheEntry(uri.toString())).hash

        changedFile.appendBytes(byteArrayOf(0))
        val changedHash = checkNotNull(FileHashGenerator(testContext).generateHash(uri.toString()))
        assertThat(changedHash == originalHash).isEqualTo(false)
        assertThat(
            visualizer.prepareVisualization(
                uri.toString(),
                lifecycleOwner,
                currentOwner,
                duration,
            ).isSuccess,
        ).isTrue()
        assertThat(bindings).isEqualTo(2)
        withTimeout(60.seconds) { visualizer.visualizerState(currentOwner).first { it == VisualizerState.FINISHED } }
        assertThat(cache.getCacheEntry(uri.toString())?.hash).isEqualTo(changedHash)
    }

    @Test
    fun extraction_failure_is_returned_to_the_caller() = runBlocking {
        val emptyAudioFile = tempFolder.newFile("empty-audio.m4a")
        currentOwner = newOwner()

        val result = visualizer.prepareVisualization(
            fileUri = Uri.fromFile(emptyAudioFile).toString(),
            lifecycleOwner = lifecycleOwner,
            session = currentOwner,
            timePerPointInMs = visualPoint.inWholeMilliseconds.toInt(),
        )

        assertThat(result.isFailure, "empty audio extraction result").isTrue()
        assertThat(visualizer.visualizerState(currentOwner).first()).isEqualTo(VisualizerState.FAILED)
        assertThat(VisualCacheImpl(tempDir).getCacheEntry(Uri.fromFile(emptyAudioFile).toString())).isNull()

        val validAudio = SyntheticAACTestAudioProvider.encodeAlternatingLoudSilence(testContext)
        currentOwner = newOwner()
        val retry = visualizer.prepareVisualization(
            fileUri = Uri.fromFile(validAudio).toString(),
            lifecycleOwner = lifecycleOwner,
            session = currentOwner,
            timePerPointInMs = visualPoint.inWholeMilliseconds.toInt(),
        )
        assertThat(retry.isSuccess, "preparation after extraction failure").isTrue()
    }

    @OptIn(UnstableApi::class)
    @Test
    fun givenDeadHandlerWhenExtractionStartsThenFailureIsReported() = runBlocking {
        val audioFile = SyntheticAACTestAudioProvider.encodeAlternatingLoudSilence(testContext)
        val thread = HandlerThread("dead-codec-handler").apply { start() }
        val handler = Handler(thread.looper)
        thread.quitSafely()
        thread.join(5_000)
        assertThat(thread.isAlive).isEqualTo(false)

        val decoder = MediaCodecPCMDataDecoder(visualPoint.inWholeMilliseconds.toInt(), handler)
        try {
            val result = runCatching {
                decoder.initiateExtraction(
                    DefaultExtractorsFactory(),
                    DefaultDataSource.Factory(testContext),
                    Uri.fromFile(audioFile),
                ).getOrThrow()
            }
            assertThat(result.isFailure).isTrue()
        } finally {
            decoder.cleanUp()
        }
    }

    @Test
    fun givenTransferredLifecycleWhenOldOwnerEndsThenDecoderThreadRemainsAlive() {
        val controller = ThreadLifecycleControllerImpl("lifecycle-transfer-test")
        val oldOwner = MutableTestLifecycleOwner()
        val newOwner = MutableTestLifecycleOwner()

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            oldOwner.registry.currentState = Lifecycle.State.RESUMED
            newOwner.registry.currentState = Lifecycle.State.RESUMED
            val handler = checkNotNull(controller.bindToLifecycle(oldOwner))
            assertThat(controller.bindToLifecycle(newOwner)).isEqualTo(handler)

            oldOwner.registry.currentState = Lifecycle.State.DESTROYED
            assertThat(handler.looper.thread.isAlive).isTrue()

            newOwner.registry.currentState = Lifecycle.State.DESTROYED
            assertThat(handler.looper.thread.isAlive).isEqualTo(false)
        }
    }

    @Test
    fun givenQueuedCleanupWhenThreadStopsThenCleanupRunsBeforeExit() {
        val controller = ThreadLifecycleControllerImpl("queued-cleanup-test")
        val handler = checkNotNull(controller.bindToLifecycle(lifecycleOwner))
        val started = CountDownLatch(1)
        val unblock = CountDownLatch(1)
        val cleaned = CountDownLatch(1)
        assertThat(
            handler.post {
                started.countDown()
                unblock.await()
            },
        ).isTrue()
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(handler.post { cleaned.countDown() }).isTrue()

        val stopping = Thread { controller.stopThread(handler, 5_000) }
        try {
            stopping.start()
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (stopping.state != Thread.State.TIMED_WAITING && stopping.isAlive && System.nanoTime() < deadline) {
                Thread.yield()
            }
            assertThat(stopping.state).isEqualTo(Thread.State.TIMED_WAITING)
            unblock.countDown()
            stopping.join(5_000)
            assertThat(cleaned.await(1, TimeUnit.SECONDS)).isTrue()
        } finally {
            unblock.countDown()
            stopping.join(5_000)
        }
    }

    @Test
    fun givenBoundLifecycleWhenStoppedOffMainThenObserverIsRemovedSafely() {
        val controller = ThreadLifecycleControllerImpl("background-stop-test")
        val owner = MutableTestLifecycleOwner()
        lateinit var handler: Handler
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            owner.registry.currentState = Lifecycle.State.RESUMED
            handler = checkNotNull(controller.bindToLifecycle(owner))
        }
        val failure = AtomicReference<Throwable?>(null)
        val stopping = Thread {
            try {
                controller.stopThread(handler)
            } catch (e: Throwable) {
                failure.set(e)
            }
        }
        try {
            stopping.start()
            stopping.join(5_000)
            assertThat(stopping.isAlive).isEqualTo(false)
            assertThat(failure.get()).isNull()
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                assertThat(owner.registry.observerCount).isEqualTo(0)
            }
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { controller.stopThread(handler) }
        }
    }

    @Test
    fun givenRealLifecycleWhenDecodingCompletesThenVisualizerFinishesAndDetaches() = runBlocking {
        val lifecycle = MutableTestLifecycleOwner()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            lifecycle.registry.currentState = Lifecycle.State.RESUMED
        }
        val audioFile = SyntheticAACTestAudioProvider.encodeAlternatingLoudSilence(testContext)
        currentOwner = newOwner()
        val result = withContext(Dispatchers.Main) {
            visualizer.prepareVisualization(
                Uri.fromFile(audioFile).toString(),
                lifecycle,
                currentOwner,
                visualPoint.inWholeMilliseconds.toInt(),
            )
        }
        assertThat(result.isSuccess).isTrue()
        withTimeout(60.seconds) {
            visualizer.visualizerState(currentOwner).first { it == VisualizerState.FINISHED }
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            assertThat(lifecycle.registry.observerCount).isEqualTo(0)
        }
    }

    @Test
    fun givenActiveFileWhenPlayerSwitchesThenTeardownPrecedesDecodingAndCacheHit() = runBlocking {
        val firstFile = SyntheticAACTestAudioProvider.encodeAlternatingLoudSilence(testContext)
        val secondFile = tempFolder.newFile("second-audio.m4a")
        firstFile.copyTo(secondFile, overwrite = true)
        val firstUri = Uri.fromFile(firstFile).toString()
        val secondUri = Uri.fromFile(secondFile).toString()
        val events = Collections.synchronizedList(mutableListOf<String>())
        val backingCache = VisualCacheImpl(tempDir)
        val cache = object : VisualizerCache {
            override suspend fun getCacheEntry(key: String): VisualizerCacheEntry? {
                events.add("get:$key")
                return backingCache.getCacheEntry(key)
            }

            override suspend fun setCacheEntry(key: String, entry: VisualizerCacheEntry) {
                backingCache.setCacheEntry(key, entry)
            }
        }
        val controller = object : ThreadController {
            val delegate = ThreadLifecycleControllerImpl("switch-test-thread")

            override fun bindToLifecycle(lifecycleOwner: LifecycleOwner): Handler? {
                events.add("bind")
                return delegate.bindToLifecycle(lifecycleOwner)
            }

            override fun stopThread(handler: Handler?, maxWaitTime: Long) {
                delegate.stopThread(handler, maxWaitTime)
                events.add("stop")
            }
        }
        visualizer = AudioVisualizerImpl(
            context = testContext,
            cache = cache,
            extractor = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true),
            dataSource = DefaultDataSource.Factory(testContext),
            threadHandler = controller,
        )

        suspend fun prepare(uri: String, owner: VisualizerSession) = visualizer.prepareVisualization(
            fileUri = uri,
            lifecycleOwner = lifecycleOwner,
            session = owner,
            timePerPointInMs = visualPoint.inWholeMilliseconds.toInt(),
        )

        val firstOwner = newOwner()
        val secondOwner = newOwner()
        currentOwner = firstOwner
        assertThat(prepare(firstUri, firstOwner).isSuccess).isTrue()
        currentOwner = secondOwner
        assertThat(prepare(secondUri, secondOwner).isSuccess).isTrue()
        val firstStop = events.indexOf("stop")
        val firstCacheRead = events.indexOf("get:$secondUri")
        assertThat(firstStop).isGreaterThanOrEqualTo(0)
        assertThat(firstStop).isLessThan(firstCacheRead)
        assertThat(firstCacheRead).isLessThan(events.lastIndexOf("bind"))
        visualizer.cleanUp(firstOwner)
        withTimeout(60.seconds) {
            visualizer.visualizerState(secondOwner).first { it == VisualizerState.FINISHED }
        }

        events.clear()
        val thirdOwner = newOwner()
        currentOwner = thirdOwner
        assertThat(prepare(firstUri, thirdOwner).isSuccess).isTrue()
        currentOwner = newOwner()
        assertThat(prepare(secondUri, currentOwner).isSuccess).isTrue()
        val secondStop = events.indexOf("stop")
        assertThat(secondStop).isGreaterThanOrEqualTo(0)
        assertThat(secondStop).isLessThan(events.indexOf("get:$secondUri"))
        assertThat(events.count { it == "bind" }).isEqualTo(1)
        assertThat(visualizer.visualizerState(currentOwner).first()).isEqualTo(VisualizerState.FINISHED)
        visualizer.cleanUp(thirdOwner)
        assertThat(visualizer.visualizerState(currentOwner).first()).isEqualTo(VisualizerState.FINISHED)
    }

    @Test
    fun givenCancelledPreparationWhenReopenedThenNewOwnerCanDecode() = runBlocking {
        val audioFile = SyntheticAACTestAudioProvider.encodeAlternatingLoudSilence(testContext)
        val uri = Uri.fromFile(audioFile).toString()
        val cacheReadStarted = CompletableDeferred<Unit>()
        val allowCacheRead = CompletableDeferred<Unit>()
        val backingCache = VisualCacheImpl(tempDir)
        val cache = object : VisualizerCache {
            override suspend fun getCacheEntry(key: String): VisualizerCacheEntry? {
                cacheReadStarted.complete(Unit)
                allowCacheRead.await()
                return backingCache.getCacheEntry(key)
            }

            override suspend fun setCacheEntry(key: String, entry: VisualizerCacheEntry) {
                backingCache.setCacheEntry(key, entry)
            }
        }
        visualizer = AudioVisualizerImpl(
            context = testContext,
            cache = cache,
            extractor = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true),
            dataSource = DefaultDataSource.Factory(testContext),
            threadHandler = ThreadLifecycleControllerImpl("cancel-test-thread"),
        )

        val cancelledOwner = newOwner()
        currentOwner = cancelledOwner
        val preparation = launch {
            visualizer.prepareVisualization(
                uri,
                lifecycleOwner,
                cancelledOwner,
                visualPoint.inWholeMilliseconds.toInt(),
            )
        }
        withTimeout(10.seconds) { cacheReadStarted.await() }
        preparation.cancelAndJoin()
        assertThat(visualizer.visualizerState(cancelledOwner).first()).isEqualTo(VisualizerState.NOT_STARTED)
        assertThat(backingCache.getCacheEntry(uri)).isNull()

        allowCacheRead.complete(Unit)
        currentOwner = newOwner()
        val retry =
            visualizer.prepareVisualization(uri, lifecycleOwner, currentOwner, visualPoint.inWholeMilliseconds.toInt())
        assertThat(retry.isSuccess).isTrue()
        withTimeout(60.seconds) {
            visualizer.visualizerState(currentOwner).first { it == VisualizerState.FINISHED }
        }
        Unit
    }

    @Test
    fun stale_cache_entry_re_decodes_the_audio() = runBlocking {
        val audioFile = SyntheticAACTestAudioProvider.encodeAlternatingLoudSilence(testContext)

        val cache = VisualCacheImpl(tempDir)
        cache.setCacheEntry(
            Uri.fromFile(audioFile).toString(),
            VisualizerCacheEntry(
                hash = "not-the-hash-of-this-file",
                entryFloats = floatArrayOf(1f),
                timePerPointMs = visualPoint.inWholeMilliseconds.toInt(),
            ),
        )
        assertThat(cache.getCacheEntry(Uri.fromFile(audioFile).toString())).isNotNull()

        val (points, _) = visualizationParser(audioFile, timeout = 60.seconds)

        // the stale points were replaced by a full fresh decode
        assertThat(points.size, "decoded points").isGreaterThan(3)

        // new entry replaced the data also being compressed the data size should be smaller
        val replaced = cache.getCacheEntry(Uri.fromFile(audioFile).toString())

        assertThat(replaced).isNotNull()
        assertThat(replaced?.entryFloats?.size)
            .isNotNull()
            .isLessThanOrEqualTo(points.size)
    }

    private fun visualizationParser(audioFile: File, timeout: Duration): Pair<FloatArray, Duration> = runBlocking {
        currentOwner = newOwner()
        val timed = measureTime {
            val result = visualizer.prepareVisualization(
                fileUri = Uri.fromFile(audioFile).toString(),
                lifecycleOwner = lifecycleOwner,
                session = currentOwner,
                timePerPointInMs = visualPoint.inWholeMilliseconds.toInt(),
            )
            assertThat(result.isSuccess, "prepareVisualization").isTrue()

            withTimeout(timeout) {
                visualizer.visualizerState(currentOwner).first { it == VisualizerState.FINISHED }
            }
        }
        visualizer.normalizedVisualization(currentOwner).first() to timed
    }

    @Test
    fun givenSameFileWhenLeaseTransfersThenOnlyCurrentOwnerSeesVisuals() = runBlocking {
        val audioFile = SyntheticAACTestAudioProvider.encodeAlternatingLoudSilence(testContext)
        val firstOwner = newOwner()
        val secondOwner = VisualizerSession(firstOwner.audioId, UUID.randomUUID().toString())
        val uri = Uri.fromFile(audioFile).toString()

        val firstResult = visualizer.prepareVisualization(
            fileUri = uri,
            lifecycleOwner = lifecycleOwner,
            session = firstOwner,
            timePerPointInMs = visualPoint.inWholeMilliseconds.toInt(),
        )
        val secondResult = visualizer.prepareVisualization(
            fileUri = uri,
            lifecycleOwner = lifecycleOwner,
            session = secondOwner,
            timePerPointInMs = visualPoint.inWholeMilliseconds.toInt(),
        )

        assertThat(firstResult.isSuccess).isTrue()
        assertThat(secondResult.isSuccess).isTrue()
        assertThat(visualizer.visualizerState(firstOwner).first()).isEqualTo(VisualizerState.NOT_STARTED)
        withTimeout(60.seconds) {
            visualizer.visualizerState(secondOwner).first { it == VisualizerState.FINISHED }
        }
        assertThat(visualizer.normalizedVisualization(firstOwner).first().isEmpty()).isTrue()
        assertThat(visualizer.normalizedVisualization(secondOwner).first().isNotEmpty()).isTrue()

        visualizer.cleanUp(firstOwner)
        assertThat(visualizer.visualizerState(secondOwner).first()).isEqualTo(VisualizerState.FINISHED)
        visualizer.cleanUpAndAwait(secondOwner)

        currentOwner = newOwner()
        val reopened = visualizer.prepareVisualization(
            fileUri = uri,
            lifecycleOwner = lifecycleOwner,
            session = currentOwner,
            timePerPointInMs = visualPoint.inWholeMilliseconds.toInt(),
        )
        assertThat(reopened.isSuccess).isTrue()
        assertThat(visualizer.visualizerState(currentOwner).first()).isEqualTo(VisualizerState.FINISHED)
    }

    private fun newOwner() = VisualizerSession(
        audioId = ++nextPlayerId,
        computeToken = UUID.randomUUID().toString(),
    )

    private class MutableTestLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
    }


    companion object {
        private const val TAG = "VISUALIZER_TEST"

        private val visualPoint = 100.milliseconds
        private val maxParsingTime = 20.seconds
        private val macCompletionTime = 5.minutes
    }
}
