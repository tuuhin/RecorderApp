package com.eva.speech_enhancements.model_manager

import android.util.Log
import com.eva.speech_enhancements.model_manager.data.SpeechModelFile
import com.eva.speech_enhancements.model_manager.data.SpeechModelMetadata
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import java.io.IOException
import kotlin.uuid.Uuid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.HashingSource
import okio.Path
import okio.blackholeSink
import okio.buffer

internal class SpeechModelDownloader(
    private val reader: SpeechModelReader,
    private val httpClient: HttpClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ISpeechModelDownloader {
    private val fs: FileSystem = FileSystem.SYSTEM

    override suspend fun download(onProgress: (Int) -> Unit): Result<Unit> = withContext(dispatcher) {
        try {
            val model = reader.readAssetsModel()
                ?: throw IllegalStateException("models.json is missing or invalid")
            downloadModel(model, onProgress)
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) {
                withContext(NonCancellable) {
                    if (fs.exists(reader.modelRoot))
                        fs.deleteRecursively(reader.modelRoot)
                }
                throw e
            }
            Log.e(TAG, "Failed to download the speech model", e)
            Result.failure(e)
        }
    }

    private suspend fun downloadModel(
        model: SpeechModelMetadata,
        onProgress: (Int) -> Unit,
    ) {
        validateMetadata(model)

        val root = reader.modelRoot
        val target = reader.modelDirectory(model)
        val token = Uuid.random().toString()

        val staging = root.resolve("${model.id}.tmp-$token")
        val backup = root.resolve("${model.id}.old-$token")

        fs.createDirectories(root)
        if (fs.exists(staging)) fs.deleteRecursively(staging)
        fs.createDirectories(staging)

        val totalBytes = model.files.sumOf { it.sizeBytes }.coerceAtLeast(1L)
        var downloadedBytes = 0L
        var lastPercent = -1

        onProgress(0)
        try {
            for (file in model.files) {
                currentCoroutineContext().ensureActive()
                Log.d(TAG, "Downloading ${file.name} (${file.sizeBytes} bytes)")

                downloadFile(
                    url = model.baseUri.trimEnd('/') + "/" + file.name,
                    destination = staging.resolve(file.name),
                    onBytes = { read ->
                        downloadedBytes += read
                        val percent = ((downloadedBytes * 100.0) / totalBytes).toInt().coerceIn(0, 100)
                        if (percent != lastPercent) {
                            lastPercent = percent
                            onProgress(percent)
                        }
                    },
                )
                verifyFile(staging.resolve(file.name), file)
            }

            // Swap the verified staging directory into place, keeping the old copy until it succeeds.
            if (fs.exists(target)) fs.atomicMove(target, backup)
            try {
                fs.atomicMove(staging, target)
            } catch (e: Exception) {
                if (fs.exists(backup) && !fs.exists(target)) fs.atomicMove(backup, target)
                throw e
            }
            withContext(NonCancellable) {
                if (fs.exists(backup)) fs.deleteRecursively(backup)
            }

            onProgress(100)
            Log.i(TAG, "Speech model installed at $target")
        } catch (e: Throwable) {
            withContext(NonCancellable) {
                runCatching { if (fs.exists(staging)) fs.deleteRecursively(staging) }
            }
            throw e
        }
    }

    private fun validateMetadata(model: SpeechModelMetadata) {
        if (!model.baseUri.startsWith("https://"))
            throw IllegalStateException("Model base URI must use https: ${model.baseUri}")

        if (model.files.isEmpty())
            throw IllegalStateException("Model ${model.id} declares no files")

        for ((name, sizeBytes, checksumSha256) in model.files) {
            val isUnsafeName = name.isBlank() || name.contains('/') || name.contains('\\') || name == ".."

            if (isUnsafeName) throw IllegalStateException("Unsafe model file name: $name")
            if (sizeBytes <= 0L) throw IllegalStateException("Invalid size for $name: $sizeBytes")
            if (!SHA256_REGEX.matches(checksumSha256.lowercase())) throw IllegalStateException("Invalid SHA-256 checksum for $name")
        }
    }

    private suspend fun downloadFile(
        url: String,
        destination: Path,
        onBytes: (Long) -> Unit,
    ) {
        httpClient.prepareGet(url) {
            // never serve a partial or stale model from a cache
            header(HttpHeaders.CacheControl, "no-cache, no-store")
            header(HttpHeaders.Pragma, "no-cache")
        }.execute { response ->

            if (!response.status.isSuccess())
                throw IOException("Model download failed with HTTP ${response.status.value}: $url")

            val channel = response.bodyAsChannel()
            destination.parent?.let { fs.createDirectories(it) }

            fs.sink(destination).buffer().use { sink ->
                val buffer = ByteArray(8 * 1024)

                while (currentCoroutineContext().isActive) {
                    val read = channel.readAvailable(buffer, 0, buffer.size)
                    if (read < 0) break
                    if (read == 0) continue
                    sink.write(buffer, 0, read)
                    onBytes(read.toLong())
                }
            }
        }
    }

    private suspend fun verifyFile(path: Path, file: SpeechModelFile) {

        val size = fs.metadata(path).size ?: 0
        if (size != file.sizeBytes)
            throw IOException("Size mismatch for ${file.name}: expected ${file.sizeBytes}, got $size")

        val actual = calculateSha256(path)
        if (!actual.equals(file.checksumSha256, ignoreCase = true))
            throw IOException("Checksum mismatch for ${file.name}: expected ${file.checksumSha256}, got $actual")
    }

    private suspend fun calculateSha256(path: Path): String = withContext(Dispatchers.Default) {
        fs.source(path).use { source ->
            HashingSource.sha256(source).use { hashingSource ->
                hashingSource.buffer().use { buffered -> buffered.readAll(blackholeSink()) }
                hashingSource.hash.hex()
            }
        }
    }

    companion object {
        private const val TAG = "SpeechModelDownloader"
        private val SHA256_REGEX = Regex("^[a-f0-9]{64}$")
    }
}
