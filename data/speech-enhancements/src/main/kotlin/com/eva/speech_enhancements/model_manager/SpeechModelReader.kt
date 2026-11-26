package com.eva.speech_enhancements.model_manager

import android.content.Context
import android.util.Log
import com.eva.speech_enhancements.model_manager.data.ModelFileStatus
import com.eva.speech_enhancements.model_manager.data.SpeechModelManifestReader
import com.eva.speech_enhancements.model_manager.data.SpeechModelMetadata
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.HashingSource
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.blackholeSink
import okio.buffer

internal class SpeechModelReader(
    private val context: Context,
    private val manifestReader: SpeechModelManifestReader,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ISpeechModelReader {
    private val fs: FileSystem = FileSystem.SYSTEM

    val modelRoot: Path
        get() = context.filesDir.toOkioPath().resolve(MODEL_ROOT_DIR)

    suspend fun readAssetsModel(): SpeechModelMetadata? {
        return try {
            manifestReader.read().models.firstOrNull()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "FAILED TO DE SERIALIZE FROM ASSETS FILE ", e)
            null
        }
    }

    fun modelDirectory(model: SpeechModelMetadata): Path = modelRoot.resolve(model.id)

    override suspend fun isDownloaded(): Boolean = withContext(ioDispatcher) {
        val model = readAssetsModel() ?: return@withContext false
        if (model.files.isEmpty()) return@withContext false
        val directory = modelDirectory(model)
        model.files.all { fs.exists(directory.resolve(it.name)) }
    }

    suspend fun getModelPath(): Result<File> = runCatching {
        val model = readAssetsModel()
            ?: throw IllegalStateException("models.json is missing or invalid")
        if (!isDownloaded()) throw IllegalStateException("The model is not downloaded")
        modelDirectory(model).toFile()
    }

    suspend fun status(): ModelFileStatus = withContext(ioDispatcher) {
        val model = readAssetsModel() ?: return@withContext ModelFileStatus.MISSING
        if (model.files.isEmpty()) return@withContext ModelFileStatus.MISSING

        val directory = modelDirectory(model)
        for ((name, sizeBytes, checksumSha256) in model.files) {
            val path = directory.resolve(name)
            if (!fs.exists(path)) return@withContext ModelFileStatus.MISSING
            if (fs.metadata(path).size != sizeBytes) return@withContext ModelFileStatus.CORRUPTED
            if (!calculateSha256(path).equals(checksumSha256, ignoreCase = true))
                return@withContext ModelFileStatus.CORRUPTED
        }
        return@withContext ModelFileStatus.VALID
    }

    private suspend fun calculateSha256(path: Path): String = withContext(Dispatchers.Default) {
        fs.source(path).use { source ->
            HashingSource.sha256(source).use { hashingSource ->
                hashingSource.buffer().use { buffered -> buffered.readAll(blackholeSink()) }
                hashingSource.hash.hex()
            }
        }
    }

    override suspend fun delete() = withContext(ioDispatcher) {
        try {
            if (fs.exists(modelRoot)) fs.deleteRecursively(modelRoot)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Failed to delete the model directory", e)
        }
    }

    companion object {
        private const val TAG = "SpeechModelReader"
        private const val MODEL_ROOT_DIR = "speech_enhancement_model"
    }
}
