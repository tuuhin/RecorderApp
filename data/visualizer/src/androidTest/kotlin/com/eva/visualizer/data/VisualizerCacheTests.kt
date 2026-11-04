package com.eva.visualizer.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.com.visualizer.data.cache.VisualCacheImpl
import com.com.visualizer.domain.VisualizerCache
import com.com.visualizer.domain.model.VisualizerCacheEntry
import java.io.File
import java.security.MessageDigest
import kotlin.random.Random
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VisualizerCacheTests {

    private lateinit var vCache: VisualizerCache
    private lateinit var cacheDir: File
    private lateinit var entriesDir: File

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Before
    fun setUp() {
        cacheDir = tempFolder.newFolder()
        entriesDir = File(cacheDir, "visualizer_cache")
        vCache = VisualCacheImpl(cacheDir)
    }

    @Test
    fun entry_round_trips_through_the_file() = runTest {
        val points = floatArrayOf(
            0f,
            0.25f,
            0.5f,
            0.75f,
            1f,
            0.1f,
            0.9f,
            0.999f,
        )

        val expected = VisualizerCacheEntry(
            hash = "hash-a",
            entryFloats = points,
            timePerPointMs = 100,
        )

        vCache.setCacheEntry("1", expected)

        val entry = vCache.getCacheEntry("1")

        assertThat(entry).isNotNull()
        assertThat(entry?.hash).isEqualTo("hash-a")
        assertThat(entry?.timePerPointMs).isEqualTo(100)
        assertThat(entry?.entryFloats?.size).isEqualTo(points.size)

        points.forEachIndexed { index, expectedPoint ->
            assertThat(entry!!.entryFloats[index])
                .isCloseTo(expectedPoint, QUANTIZATION_TOLERANCE)
        }
    }

    @Test
    fun silent_entry_round_trips_as_zeros() = runTest {
        val expected = VisualizerCacheEntry(
            hash = "hash-b",
            entryFloats = FloatArray(4),
            timePerPointMs = 100,
        )

        vCache.setCacheEntry("2", expected)

        val entry = vCache.getCacheEntry("2")

        assertThat(entry).isNotNull()
        assertThat(entry?.hash).isEqualTo("hash-b")
        assertThat(entry?.timePerPointMs).isEqualTo(100)
        assertThat(entry?.entryFloats?.toList())
            .isEqualTo(listOf(0f, 0f, 0f, 0f))
    }

    @Test
    fun missing_entry_returns_null() = runTest {
        assertThat(vCache.getCacheEntry("missing")).isNull()
    }

    @Test
    fun corrupt_file_is_removed_and_reports_null() = runTest {
        vCache.setCacheEntry(
            "4",
            VisualizerCacheEntry(
                hash = "hash-c",
                entryFloats = floatArrayOf(1f),
                timePerPointMs = 100,
            ),
        )


        val file = findEntryFileFor("4")

        assertThat(file.isFile).isTrue()

        file.writeBytes(byteArrayOf(1, 2, 3, 4))

        assertThat(vCache.getCacheEntry("4")).isNull()
        assertThat(file.exists()).isFalse()
    }

    @Test
    fun budget_prunes_oldest_files_first_and_keeps_newest() = runTest {
        // Write entries without triggering pruning.
        val unlimitedCache = VisualCacheImpl(
            cacheDir = cacheDir,
            maxCacheBytes = Long.MAX_VALUE,
        )

        unlimitedCache.setCacheEntry("5", entry(21L))
        val oldest = findEntryFileFor("5")

        unlimitedCache.setCacheEntry("6", entry(22L))
        val middle = findEntryFileFor("6")

        unlimitedCache.setCacheEntry("7", entry(23L))
        val newest = findEntryFileFor("7")

        assertThat(oldest.isFile).isTrue()
        assertThat(middle.isFile).isTrue()
        assertThat(newest.isFile).isTrue()

        // Assign deterministic timestamps. All entries are written before
        // their timestamps are modified.
        assertThat(oldest.setLastModified(1_000L)).isTrue()
        assertThat(middle.setLastModified(2_000L)).isTrue()
        assertThat(newest.setLastModified(3_000L)).isTrue()

        val oldestSize = oldest.length()
        val middleSize = middle.length()
        val newestSize = newest.length()

        // Measure entry 8's size
        unlimitedCache.setCacheEntry("8", entry(24L))
        val entry8 = findEntryFileFor("8")
        val entry8Size = entry8.length()
        entry8.delete()

        // Keep the two newest entries, plus the incoming entry, but not all four.
        val budget = middleSize + newestSize + entry8Size

        assertThat(budget < oldestSize + middleSize + newestSize + entry8Size).isTrue()

        // A fourth write triggers pruning. The budget allows the two newer
        // entries, so the oldest entry should be removed first.
        val pruningCache = VisualCacheImpl(
            cacheDir = cacheDir,
            maxCacheBytes = budget,
        )

        pruningCache.setCacheEntry("8", entry(24L))

        // The newly written entry is protected by the implementation's
        // keep parameter. Therefore, pruning removes old entries until
        // the remaining files fit the budget, where possible.
        assertThat(oldest.exists()).isFalse()
        assertThat(middle.exists()).isTrue()
        assertThat(newest.exists()).isTrue()
        assertThat(entry8.exists()).isTrue()

        assertThat(pruningCache.getCacheEntry("6")).isNotNull()
        assertThat(pruningCache.getCacheEntry("7")).isNotNull()
        assertThat(pruningCache.getCacheEntry("8")).isNotNull()
    }

    private fun entry(id: Long): VisualizerCacheEntry {
        val random = Random(id)

        return VisualizerCacheEntry(
            hash = "hash-$id",
            entryFloats = FloatArray(2_048) { random.nextFloat() },
            timePerPointMs = 100,
        )
    }

    private fun findEntryFileFor(key: String): File {
        val hex = MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return File(entriesDir, "$hex.vlz")
    }

    companion object {
        private const val QUANTIZATION_TOLERANCE = 2e-5f
    }
}
