package com.eva.visualizer.data

import android.content.Context
import android.media.MediaMetadataRetriever
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

fun Context.copyAssetToCache(assetName: String): File {
    val destination = File(cacheDir, assetName)
    if (destination.exists()) return destination

    val temp = File(destination.parentFile, destination.name + ".tmp")
    assets.open(assetName).use { input ->
        temp.outputStream().use { output -> input.copyTo(output) }
    }
    check(temp.renameTo(destination)) { "could not move asset into place" }
    return destination
}

fun File.readInFileDuration(): Duration {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(absolutePath)
        val durationMs = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull()
        checkNotNull(durationMs) { "cannot read duration of $name" }
        return durationMs.milliseconds
    } finally {
        retriever.release()
    }
}
