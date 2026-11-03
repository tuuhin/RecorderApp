package com.eva.transcribe.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.eva.transcribe.domain.ModelDownloadManager
import com.eva.transcribe.domain.exceptions.ModelDownloadFailedException
import com.eva.transcribe.domain.models.ModelDownloadState
import com.eva.transcribe.domain.models.STTModelState
import com.eva.transcribe.domain.repository.STTModelsRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import java.io.IOException
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.FileSystem
import okio.HashingSource
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import okio.blackholeSink
import okio.buffer
import okio.openZip

internal class ModelDownloadMangerImpl(
    private val context: Context,
    private val httpClient: HttpClient,
    private val repository: STTModelsRepository,
) : ModelDownloadManager {

    private val fs = FileSystem.SYSTEM
    private val modelFolder by lazy { context.filesDir.toOkioPath().resolve(ModelConstants.MODEL_FILES_DIR) }
    private val cachePath by lazy { context.cacheDir.toOkioPath() }

    override suspend fun downloadAndSaveModel(
        modelId: String,
        onDownloadState: suspend (ModelDownloadState) -> Unit,
    ): Result<Boolean> {
        return try {
            Result.success(performDownload(modelId, onDownloadState))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "DOWNLOAD FAILED: $modelId", e)
            Result.failure(e)
        }
    }

    private suspend fun performDownload(
        modelId: String,
        onDownloadState: suspend (ModelDownloadState) -> Unit,
    ): Boolean {
        // fetches the model by model id from the db
        val modelResult = repository.readModelByLanguage(modelId).getOrThrow()

        // check the previous installation path
        val previousInstallation = modelResult.localModelURI
            ?.let(Uri::parse)
            ?.let { uri ->
                when (uri.scheme) {
                    "file" -> uri.path?.toPath()
                    null -> uri.toString().toPath()
                    else -> null
                }
            }
            ?.takeIf { it.parent == modelFolder }

        // check to ensure we dont have a wrong model uri
        if (modelResult.downloadURI.isBlank()) throw IllegalStateException("Invalid model download uri")
        if (modelResult.name.isBlank() && modelResult.downloadedVersion.isBlank())
            throw IllegalStateException("Invalid model metadata")

        // checksum pattern check
        val expectedChecksum = modelResult.checksum
            ?.trim()
            ?.lowercase()
            ?.takeIf { SHA256_REGEX.matches(it) }
            ?: throw IllegalStateException("A valid SHA-256 checksum is required")

        // install path
        val installationDirectory = modelFolder.resolve("${modelResult.modelId}_${UUID.randomUUID()}")
        val extractionDirectory = modelFolder.resolve("${installationDirectory.name}.tmp")
        val zipCacheFile = cachePath.resolve("model_${modelResult.modelId}_${UUID.randomUUID()}.zip")

        var installationStarted = false
        var markedDownloading = false
        var committed = false

        try {
            onDownloadState(ModelDownloadState.DownloadInitiated)
            Log.d(TAG, "STARTING DOWNLOAD: ${modelResult.modelId}")

            markedDownloading = true
            repository.updateSTTModel(modelResult.copy(state = STTModelState.DOWNLOADING)).getOrThrow()

            fs.createDirectories(modelFolder)

            downloadModelZipFile(modelResult.downloadURI, zipCacheFile) { progress ->
                onDownloadState(ModelDownloadState.DownloadProgress(progress))
            }

            Log.d(TAG, "VERIFYING MODEL CHECKSUM")
            val actualChecksum = calculateSha256Hash(zipCacheFile)
            if (!actualChecksum.equals(expectedChecksum, ignoreCase = true))
                throw IllegalStateException("Model checksum verification failed")


            Log.d(TAG, "MODEL CHECKSUM VERIFIED")
            onDownloadState(ModelDownloadState.ModelUnzipping)

            extractModelArchive(zipCacheFile, extractionDirectory)

            // zip file is moved to extraction dir
            withContext(NonCancellable) { deleteFileSafely(zipCacheFile) }

            currentCoroutineContext().ensureActive()

            installationStarted = true

            // move the model path
            fs.atomicMove(extractionDirectory, installationDirectory)
            onDownloadState(ModelDownloadState.ModelReadyToSave)

            val updatedModel = modelResult.copy(
                state = STTModelState.DOWNLOADED,
                localModelURI = Uri.Builder()
                    .scheme("file")
                    .path(installationDirectory.toString())
                    .build()
                    .toString(),
            )
            repository.updateSTTModel(updatedModel).getOrThrow()

            committed = true
            installationStarted = false
            markedDownloading = false
            Log.d(TAG, "MODEL INSTALLED SUCCESSFULLY: ${modelResult.modelId}")

            // remove the old installed file as we are done with everything
            withContext(NonCancellable) {
                previousInstallation
                    ?.takeIf { it != installationDirectory }
                    ?.let { deleteFileSafely(it) }
            }

            onDownloadState(ModelDownloadState.ModelSaved)
            return true
        } catch (e: Throwable) {
            withContext(NonCancellable) {
                if (committed) return@withContext
                // delete the extraction dir
                deleteFileSafely(extractionDirectory)
                // if installed started remove the installation dir
                if (installationStarted) deleteFileSafely(installationDirectory)

                if (!markedDownloading) return@withContext
                // restore the original state read on load
                runCatching { repository.updateSTTModel(modelResult) }
                    .onFailure { Log.e(TAG, "FAILED TO RESTORE MODEL STATE", it) }
            }
            throw e
        } finally {
            withContext(NonCancellable) { deleteFileSafely(zipCacheFile) }
        }
    }

    private suspend fun downloadModelZipFile(
        zipFileURL: String,
        outputFilePath: Path,
        onProgress: suspend (Int) -> Unit,
    ) {
        var lastProgress = -1

        httpClient.prepareGet(zipFileURL) {
            // no caching
            header(HttpHeaders.CacheControl, "no-cache, no-store")
            header(HttpHeaders.Pragma, "no-cache")

            onDownload { readBytes, totalBytes ->
                val total = totalBytes ?: 0
                if (total > 0) {
                    val progress = ((readBytes.toFloat() / total) * 100).roundToInt()
                    if (lastProgress != progress) {
                        lastProgress = progress
                        onProgress(progress)
                    }
                }
            }
        }.execute { response ->
            if (!response.status.isSuccess()) throw ModelDownloadFailedException()

            val total = response.contentLength() ?: -1L
            Log.d(TAG, "TOTAL DOWNLOAD SIZE :${total}")

            val channel = response.bodyAsChannel()

            withContext(Dispatchers.IO) {

                fs.sink(outputFilePath).buffer().use { sink ->
                    val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                    while (!channel.isClosedForRead && isActive) {
                        val read = channel.readAvailable(buffer, 0, buffer.size)
                        if (read < 0) break
                        if (read == 0) continue
                        sink.write(buffer, 0, read)
                    }
                }
            }
            Log.d(TAG, "FILE SAVED SUCCESSFULLY AT $outputFilePath")
        }
    }

    private suspend fun calculateSha256Hash(filePath: Path): String = withContext(Dispatchers.IO) {
        fs.source(filePath).use { source ->
            HashingSource.sha256(source).use { hashingSource ->
                hashingSource.buffer().use { bufferedSource ->
                    bufferedSource.readAll(blackholeSink())
                }
                hashingSource.hash.hex()
            }
        }
    }

    private suspend fun extractModelArchive(zipFile: Path, destination: Path) = withContext(Dispatchers.IO) {

        fs.createDirectories(destination)

        var totalSize = 0L
        var entryCount = 0

        fs.openZip(zipFile).use { zipFs ->
            val rootPath = "/".toPath()

            val entries = zipFs.listRecursively(rootPath)
                .filter { it != rootPath }
                .toList()

            val topLevelEntries = entries
                .map { it.relativeTo(rootPath).toString().substringBefore('/') }
                .distinct()

            val singleRootDirectory = topLevelEntries.singleOrNull()
                ?.takeIf { root ->
                    entries.all { path ->
                        val relative = path.relativeTo(rootPath).toString()
                        relative == root || relative.startsWith("$root/")
                    } && zipFs.metadata(entries.first { it.relativeTo(rootPath).toString() == root }).isDirectory
                }

            for (path in entries) {
                ensureActive()

                entryCount++
                // fail-safe against a zip bomb
                if (entryCount > MAX_ZIP_ENTRIES)
                    throw IOException("Archive contains too many entries")

                val relativePath = path.relativeTo(rootPath).toString()
                if (relativePath.isEmpty()) continue

                val normalizedPath = relativePath.replace('\\', '/')

                // Reject absolute paths and parent-directory traversal.
                if (normalizedPath.startsWith("/") || normalizedPath.split("/").any { it == ".." })
                    throw SecurityException("Unsafe ZIP entry: $relativePath")

                // Remove the common root directory when present.
                val outputPath = if (singleRootDirectory != null)
                    normalizedPath.removePrefix("$singleRootDirectory/").takeIf { it.isNotEmpty() } ?: continue
                else normalizedPath

                val outputFile = destination.resolve(outputPath)

                if (zipFs.metadata(path).isDirectory) {
                    fs.createDirectories(outputFile)
                    continue
                }

                outputFile.parent?.let(fs::createDirectories)

                zipFs.source(path).buffer().use { input ->
                    fs.sink(outputFile).buffer().use { output ->
                        val buffer = Buffer()
                        while (true) {
                            ensureActive()
                            val read = input.read(buffer, BUFFER_SIZE.toLong())
                            if (read == -1L) break
                            totalSize += read
                            if (totalSize > MAX_UNCOMPRESSED_SIZE)
                                throw IOException("Extracted model exceeds maximum allowed size")
                            output.write(buffer, read)
                        }
                    }
                }
            }
        }
        Log.d(TAG, "MODEL EXTRACTION SUCCESSFUL: $totalSize bytes")
        Unit
    }


    private suspend fun deleteFileSafely(filePath: Path) = withContext(Dispatchers.IO) {
        try {
            if (fs.exists(filePath)) fs.deleteRecursively(filePath)
        } catch (e: Exception) {
            Log.e(TAG, "ERROR WHILE DELETING: $filePath", e)
        }
        Unit
    }

    companion object {
        private const val TAG = "ModelDownloadManager"
        private const val BUFFER_SIZE = 32 * 1024
        private const val DOWNLOAD_BUFFER_SIZE = 64 * 1024

        // max entries to protect against zip bomb
        private const val MAX_ZIP_ENTRIES = 100_000

        // models should be smaller than 1 GB once extracted
        private const val MAX_UNCOMPRESSED_SIZE = 1024L * 1024L * 1024L

        private val SHA256_REGEX = Regex("^[a-f0-9]{64}$")
    }
}
