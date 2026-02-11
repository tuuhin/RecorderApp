package com.eva.transcribe.data

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.eva.transcribe.domain.ModelDownloadManager
import com.eva.transcribe.domain.exceptions.ModelDownloadFailedException
import com.eva.transcribe.domain.models.ModelDownloadState
import com.eva.transcribe.domain.models.STTModelState
import com.eva.transcribe.domain.repository.STTModelsRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.CancellationException
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import okio.buffer
import okio.openZip
import java.io.File
import java.io.IOException

private const val TAG = "MODEL_DOWNLOAD_MANAGER"

internal class ModelDownloadMangerImpl(
	private val context: Context,
	private val httpClient: HttpClient,
	private val repository: STTModelsRepository,
) : ModelDownloadManager {

	private val modelFolder by lazy {
		File(context.filesDir, ModelConstants.MODEL_FILES_DIR)
			.apply { mkdirs() }
	}

	override suspend fun downloadAndSaveModel(
		modelId: String,
		onDownloadState: suspend (ModelDownloadState) -> Unit,
	): Result<Boolean> {

		val modelResult = repository.readModelByLanguage(modelId)
		if (modelResult.isFailure) {
			val ex = modelResult.exceptionOrNull() ?: Exception("Invalid result")
			return Result.failure(ex)
		}

		val readModel = modelResult.getOrThrow()
		val language = readModel.locale
		val downloadURI = readModel.downloadURI
		val folder = "${readModel.modelId}-${readModel.downloadedVersion}"

		val prefix = "models_dn_${language}"

		// this doesn't create the file so no need io context wrapper
		val zipFile = File.createTempFile(prefix, ".zip", context.cacheDir)
		val targetFile = File(modelFolder, folder)

		// delete the file if its already present
		if (targetFile.exists()) {
			Log.d(TAG, "DELETING CONTENT FROM THE PREVIOUS FILE")
			val result = deleteDownloadedFile(targetFile)

			if (result.isFailure) {
				val exception = result.exceptionOrNull() as? Exception ?: Exception("Failed")
				return Result.failure(exception)
			}
		}

		onDownloadState(ModelDownloadState.DownloadRequested)

		// download the file
		val result = downloadModelZipFile(downloadURI, zipFile) { progress ->
			onDownloadState(ModelDownloadState.DownloadProgress(progress))
		}
		// failed to download the file or failed to save the file
		if (result.isFailure) {
			val exc = result.exceptionOrNull() as? Exception ?: Exception("Some exception")
			return Result.failure(exc)
		}

		try {
			// un zip the file content
			onDownloadState(ModelDownloadState.ModelUnzipping)
			unZipFileContent(zipFile, targetFile)
		} finally {
			withContext(NonCancellable) {
				deleteDownloadedFile(zipFile)
			}
		}
		onDownloadState(ModelDownloadState.ModelSaved)

		try {
			val updatedMOdel = readModel.copy(
				state = STTModelState.DOWNLOADED,
				size = targetFile.totalSpace,
				localModelURI = targetFile.toUri().toString()
			).copy()
			repository.updateSTTModel(updatedMOdel)
		} catch (e: Exception) {
			if (e is CancellationException) throw e
		}
		return Result.success(true)
	}


	private suspend fun downloadModelZipFile(
		zipFileURL: String,
		outputFile: File,
		onProgress: suspend (Float) -> Unit = {}
	): Result<Boolean> {
		return try {
			val statement = httpClient.prepareGet(zipFileURL) {
				onDownload { readBytes, totalBytes ->
					val total = totalBytes ?: 0
					if (total > 0) {
						val progress = readBytes.toFloat() / total
						onProgress(progress)
					}
				}
			}
			val response = statement.execute()
			if (response.status.value != 200) return Result.failure(ModelDownloadFailedException())
			val channel = response.bodyAsChannel()
			val result = channel.saveToFS(outputFile)
			Result.success(result)
		} catch (e: Exception) {
			if (e is CancellationException) throw e
			Result.failure(e)
		}
	}


	private suspend fun ByteReadChannel.saveToFS(file: File, bufferSize: Int = 10 * 1024): Boolean {
		val filePath = file.toOkioPath()
		val fs = FileSystem.SYSTEM
		return withContext(Dispatchers.IO) {
			fs.sink(filePath).buffer().use { sink ->
				val buffer = ByteArray(bufferSize)
				try {
					while (!isClosedForRead) {
						val read = readAvailable(buffer)
						if (read == -1) break
						sink.write(buffer, 0, read)
					}
					sink.flush()
					Log.d(TAG, "FILE CONTENT COPIED")
					true
				} catch (e: IOException) {
					Log.d(TAG, "FAILED TO SAVE THE FILE", e)
					false
				}
			}
		}
	}


	suspend fun unZipFileContent(zipFile: File, targetFile: File) {

		val fileSystem = FileSystem.SYSTEM
		val zipPath = zipFile.toOkioPath()
		val targetPath = targetFile.toOkioPath()

		withContext(Dispatchers.IO) {
			try {
				fileSystem.openZip(zipPath).use { fs ->
					val fsEntries = fs.listRecursively(" / ".toPath())

					for (entry in fsEntries) {
						val metadata = fs.metadata(entry)
						val targetFilePath = targetPath / entry.toString().removePrefix("/")

						if (metadata.isDirectory) {
							fileSystem.createDirectories(targetFilePath)
						} else {
							targetFilePath.parent?.let { fileSystem.createDirectories(it) }
							fs.read(entry) {
								fileSystem.write(targetFilePath) {
									writeAll(this@read)
								}
							}
						}
					}
					Log.d(TAG, "UNZIPPED INPUT FILE:$zipFile TARGET FILE:$targetFile SUCCESS")
				}
			} catch (e: Exception) {
				if (e is CancellationException) throw e
				Log.e(TAG, "SOME EXCEPTION OCCURRED WHILE UNZIPPING", e)
			}
		}
	}


	private suspend fun deleteDownloadedFile(file: File): Result<Unit> {
		return withContext(Dispatchers.IO) {
			try {
				val isDeleted = file.deleteRecursively()
				Log.d(TAG, "FILE PATH:${file.path} DELETED :$isDeleted")
				if (isDeleted) Result.success(Unit)
				else Result.failure(Exception("Failed to delete the file"))
			} catch (e: IOException) {
				Log.e(TAG, "FAILED TO DELETE FILE", e)
				Result.failure(e)
			}
		}
	}
}