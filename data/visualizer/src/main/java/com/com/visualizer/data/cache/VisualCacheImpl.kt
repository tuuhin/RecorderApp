package com.com.visualizer.data.cache

import android.util.Log
import com.com.visualizer.domain.VisualizerCache
import com.com.visualizer.domain.model.VisualizerCacheEntry
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath

internal class VisualCacheImpl(
    cacheDir: File,
    maxCacheBytes: Long = MAX_CACHE_DIR_BYTES,
    private val protoBuf: ProtoBuf = ProtoBuf,
) : VisualizerCache {

    private val fs = FileSystem.SYSTEM
    private val cachePath = cacheDir.toOkioPath() / CACHE_DIRECTORY
    private val cacheBudget = maxCacheBytes.coerceAtLeast(0L)

    override suspend fun getCacheEntry(key: String): VisualizerCacheEntry? = withContext(Dispatchers.IO) {

        val file = entryPath(key)

        try {
            if (!fs.exists(cachePath)) fs.createDirectories(cachePath)

            val metadata = fs.metadataOrNull(file) ?: return@withContext null
            if (!metadata.isRegularFile) return@withContext null

            val bytes = fs.read(file) { readByteArray() }
            val proto = protoBuf.decodeFromByteArray<VisualizerCacheProtoEntry>(bytes)

            VisualizerCacheEntry(
                hash = proto.hash,
                entryFloats = VisualizerCacheCompressor.decode(proto),
                timePerPointMs = proto.timePerPointMs,
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "Unable to read cache entry for key $key", e)
            withContext(NonCancellable) {
                fs.delete(file, mustExist = false)
            }
            null
        }
    }

    override suspend fun setCacheEntry(key: String, entry: VisualizerCacheEntry) {
        withContext(Dispatchers.IO) {

            val destination = entryPath(key)
            val temporary = temporaryPath(key)

            try {
                if (!fs.exists(cachePath)) fs.createDirectories(cachePath)

                val encoded = VisualizerCacheCompressor.encode(entry.entryFloats)

                val proto = VisualizerCacheProtoEntry(
                    hash = entry.hash,
                    timePerPointMs = entry.timePerPointMs,
                    amplitudeScale = encoded.scale,
                    pointCount = encoded.pointCount,
                    points = encoded.payload,
                )

                val bytes = protoBuf.encodeToByteArray<VisualizerCacheProtoEntry>(proto)

                fs.write(temporary) {
                    write(bytes, 0, bytes.size)
                }

                fs.atomicMove(temporary, destination)

                pruneToBudget(keep = destination)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Unable to write cache entry for key $key", e)
            } finally {
                withContext(NonCancellable) {
                    fs.delete(temporary, mustExist = false)
                }
            }
        }
    }


    private suspend fun pruneToBudget(keep: Path) {

        // we sort the files via last modified so the file which was last access
        // so least recently used file is removed first
        val entries = try {
            fs.list(cachePath)
                .mapNotNull { path ->
                    val metadata = fs.metadataOrNull(path) ?: return@mapNotNull null
                    if (!metadata.isRegularFile || !path.name.endsWith(EXT))
                        return@mapNotNull null
                    CacheFile(
                        path = path,
                        size = metadata.size ?: 0L,
                        lastModified = metadata.lastAccessedAtMillis ?: 0L,
                    )
                }
                .sortedWith(compareBy<CacheFile> { it.lastModified }.thenBy { it.path.name })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Unable to enumerate cache entries for pruning", e)
            return
        }

        var totalBytes = entries.sumOf { it.size }
        if (totalBytes <= cacheBudget) return

        for ((path) in entries) {
            currentCoroutineContext().ensureActive()

            if (totalBytes <= cacheBudget) break
            if (path == keep) continue

            try {
                val currentMetadata = fs.metadataOrNull(path) ?: continue
                if (!currentMetadata.isRegularFile) continue
                val currentSize = currentMetadata.size ?: 0L

                // delete the file entry
                fs.delete(path, mustExist = false)

                // update the size
                totalBytes = (totalBytes - currentSize).coerceAtLeast(0L)

                Log.d(TAG, "Pruned cache entry: ${path.name}")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // A failed deletion must not be counted as reclaimed space.
                Log.w(TAG, "Unable to prune cache entry: ${path.name}", e)
            }
        }

        if (totalBytes > cacheBudget) {
            Log.w(TAG, "Cache remains above budget: $totalBytes bytes; limit is $cacheBudget bytes")
        }
    }

    private fun entryPath(key: String): Path {
        return cachePath / "${key.toSha256()}$EXT"
    }

    private fun temporaryPath(key: String): Path {
        return cachePath / "${key.toSha256()}$TEMP_FILE_SUFFIX"
    }

    private fun String.toSha256(): String =
        MessageDigest.getInstance("SHA-256")
            .digest(toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private data class CacheFile(
        val path: Path,
        val size: Long,
        val lastModified: Long,
    )

    private companion object {
        const val TAG = "VISUALIZER_CACHE"

        const val CACHE_DIRECTORY = "visualizer_cache"
        const val EXT = ".vlz"
        const val TEMP_FILE_SUFFIX = ".tmp"

        const val MAX_CACHE_DIR_BYTES = 20L * 1024L * 1024L
    }
}
