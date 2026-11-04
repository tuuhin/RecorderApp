package com.com.visualizer.data.cache

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.HashingSink
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.Source
import okio.blackholeSink
import okio.buffer
import okio.source

internal class FileHashGenerator(private val context: Context) {

    private val fs = FileSystem.SYSTEM

    suspend fun generateHash(uri: String): String? = withContext(Dispatchers.IO) {
        val uri = uri.toUri()
        try {
            when (uri.scheme) {
                "file" -> uri.toFilePath()?.let(::hashFile)

                "content" -> context.contentResolver.openInputStream(uri)
                    ?.use { stream -> stream.source().use(::hashSource) }

                else -> null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private fun hashFile(path: Path): String = fs.source(path).use { source -> hashSource(source) }

    private fun hashSource(source: Source): String =
        HashingSink.sha256(blackholeSink()).use { hashingSink ->
            hashingSink.buffer().use { sink -> sink.writeAll(source) }
            hashingSink.hash.hex()
        }

    private fun Uri.toFilePath(): Path? {
        if (!isAbsolute || scheme != "file") return null
        return toFile().toOkioPath()
    }
}
