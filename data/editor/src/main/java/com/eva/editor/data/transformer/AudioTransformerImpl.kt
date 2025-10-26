package com.eva.editor.data.transformer

import android.content.Context
import android.text.format.Formatter
import android.webkit.MimeTypeMap
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.Transformer
import com.eva.editor.BuildConfig
import com.eva.editor.domain.AudioConfigToActionList
import com.eva.editor.domain.AudioTransformer
import com.eva.editor.domain.TransformationProgress
import com.eva.editor.domain.exceptions.ExportFileException
import com.eva.editor.domain.exceptions.TransformRunningException
import com.eva.editor.domain.exceptions.TransformerInvalidException
import com.eva.editor.domain.exceptions.TransformerWrongMimeTypeException
import com.eva.recordings.domain.models.AudioFileModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

private const val TAG = "AUDIO_TRANSFORMER"

@UnstableApi
internal class AudioTransformerImpl(private val context: Context) : AudioTransformer {

	private var _transformer: Transformer? = null
	private val _isTransforming = MutableStateFlow(false)

	private val _transformerDirectory: File
		get() = File(context.cacheDir, "transformers")
			.apply(File::mkdirs)

	private val _transformerMimetypes = arrayOf(
		MimeTypes.AUDIO_AAC,
		MimeTypes.AUDIO_AMR_NB,
		MimeTypes.AUDIO_AMR_WB
	)

	private val _notSeekableFormats = arrayOf(
		MimeTypes.AUDIO_AMR,
		MimeTypes.AUDIO_AMR_NB,
		MimeTypes.AUDIO_AMR_WB,
		MimeTypes.AUDIO_MIDI,
		MimeTypes.AUDIO_EXOPLAYER_MIDI,
	)

	private val _mimeMap = MimeTypeMap.getSingleton()

	override val isTransformationRunning: StateFlow<Boolean>
		get() = _isTransforming

	@OptIn(ExperimentalCoroutinesApi::class)
	override val transformationProgress: Flow<TransformationProgress>
		get() = _isTransforming.flatMapLatest { isRunning ->
			_transformer?.transformerProgress(isRunning) ?: emptyFlow()
		}
			.catch { err -> Log.d(TAG, "ERROR IN PROGRESS ", err) }
			.onStart { emit(TransformationProgress.Idle) }
			.distinctUntilChanged()

	private fun prepareTransformer(outputMimeType: String? = null): Result<Unit> {

		val mimeType = _transformerMimetypes.find { it == outputMimeType } ?: MimeTypes.AUDIO_AAC

		return try {
			_transformer = Transformer.Builder(context)
				.setAudioMimeType(mimeType)
				.experimentalSetTrimOptimizationEnabled(true)
				.build()

			Log.d(TAG, "PREPARED TRANSFORMER, OUTPUT MIME TYPE:$mimeType")
			Result.success(Unit)
		} catch (_: IllegalStateException) {
			Result.failure(TransformerWrongMimeTypeException())
		} catch (e: Exception) {
			Result.failure(e)
		}
	}

	override suspend fun transformAudio(model: AudioFileModel, actions: AudioConfigToActionList)
			: Result<File> {

		val outputMimetype = when (model.mimeType) {
			MimeTypes.AUDIO_AMR_NB, MimeTypes.AUDIO_AMR -> MimeTypes.AUDIO_AMR_NB
			MimeTypes.AUDIO_AMR_WB -> MimeTypes.AUDIO_AMR_WB
			else -> MimeTypes.AUDIO_AAC
		}

		if (_transformer == null) {
			val result = prepareTransformer(outputMimetype)
			if (result.isFailure) {
				val exception = result.exceptionOrNull()!!
				return Result.failure(exception)
			}
		}

		if (_isTransforming.value) {
			Log.d(TAG, "TRANSFORMATION RUNNING CANNOT EDIT..")
			return Result.failure(TransformRunningException())
		}

		return try {
			_isTransforming.update { true }
			val seekable = model.mimeType !in _notSeekableFormats
			Log.d(TAG, "IS MEDIA SEEKABLE :$seekable")
			if (seekable) createFileAndTransform(model, actions)
			else createFileAndTransformIntoAcc(model, actions)
		} catch (_: IllegalArgumentException) {
			Result.failure(TransformerWrongMimeTypeException())
		} catch (e: IllegalStateException) {
			e.printStackTrace()
			Result.failure(TransformRunningException())
		} catch (e: Exception) {
			Result.failure(e)
		} finally {
			_isTransforming.update { false }
		}
	}

	override suspend fun removeTransformsFile(uri: String): Result<Boolean> {
		return withContext(Dispatchers.IO) {
			try {
				val file = uri.toUri().toFile()
				if (!file.exists())
					return@withContext Result.failure(Exception("Wrong file provided"))
				if (file.parentFile == _transformerDirectory)
					return@withContext Result.failure(Exception("Invalid file location"))
				val delete = file.delete()
				Log.d(TAG, "TEMP REMOVED :$delete")
				Result.success(delete)
			} catch (e: Exception) {
				e.printStackTrace()
				Result.failure(e)
			}
		}
	}


	private suspend fun createFileAndTransform(
		model: AudioFileModel,
		actions: AudioConfigToActionList
	): Result<File> {


		val transformer = _transformer ?: return Result.failure(TransformerInvalidException())

		return try {
			val file = withContext(Dispatchers.IO) {
				val extension = _mimeMap.getExtensionFromMimeType(model.mimeType)
				File.createTempFile("temp_", ".$extension", _transformerDirectory)
			}
			try {
				// if not acc type
				val composition = model.toComposition(actions)
				transformer.awaitResults(composition, file.path)
				if (file.exists() && BuildConfig.DEBUG) {
					val fileSize = Formatter.formatFileSize(context, file.length())
					Log.d(TAG, "FILE CREATED :NAME ${file.name} SIZE:$fileSize")
				}
				Result.success(file)
			} catch (e: CancellationException) {
				// delete the file as this export was cancelled
				withContext(NonCancellable) { file.delete() }
				throw e
			} catch (e: ExportException) {
				// delete the file as this export failed
				withContext(NonCancellable) { file.delete() }
				Result.failure(e)
			}
		} catch (e: IOException) {
			Log.e(TAG, "FAILED TO CREATE TEMP FILE", e)
			Result.failure(ExportFileException())
		} catch (e: Exception) {
			if (e is CancellationException) throw e
			Result.failure(e)
		}
	}

	private suspend fun createFileAndTransformIntoAcc(
		model: AudioFileModel,
		actions: AudioConfigToActionList
	): Result<File> {

		val transformer = _transformer ?: return Result.failure(TransformerInvalidException())

		val aacTransformer = transformer.buildUpon()
			.setAudioMimeType(MimeTypes.AUDIO_AAC)
			.build()

		return try {
			val firstTransform = withContext(Dispatchers.IO) {
				val extension = _mimeMap.getExtensionFromMimeType(MimeTypes.AUDIO_AAC)
				File.createTempFile("first_conversion", ".$extension", context.cacheDir)
			}
			val finalFile = withContext(Dispatchers.IO) {
				val extension = _mimeMap.getExtensionFromMimeType(model.mimeType)
				File.createTempFile("final_conversion_", ".$extension", context.cacheDir)
			}
			try {
				// convert the file into acc
				val mediaItem = MediaItem.fromUri(model.fileUri)
				val firstConversion = aacTransformer.awaitResults(mediaItem, firstTransform.path)

				Log.d(TAG, "CONVERTED FILE TO ACC FORMAT")

				// prepare the composition
				val newModel = model.copy(fileUri = firstConversion)
				val composition = newModel.toComposition(actions = actions)

				// final transformation
				transformer.awaitResults(composition, finalFile.path)

				Log.d(TAG, "TRANSFORMATION COMPLETED")

				if (finalFile.exists() && BuildConfig.DEBUG) {
					val fileSize = Formatter.formatFileSize(context, finalFile.length())
					Log.d(TAG, "FILE CREATED :NAME ${finalFile.name} SIZE:$fileSize")
				}

				Result.success(finalFile)
			} catch (e: CancellationException) {
				// delete the file as this export was cancelled
				withContext(NonCancellable) {
					if (finalFile.exists()) finalFile.delete()
				}
				throw e
			} catch (e: ExportException) {
				e.printStackTrace()
				// delete the file as this export failed
				withContext(NonCancellable) {
					if (finalFile.exists()) finalFile.delete()
				}
				Result.failure(e)
			} catch (e: Exception) {
				e.printStackTrace()
				Result.failure(e)
			} finally {
				// first transform should be deleted everytime
				withContext(NonCancellable) {
					if (firstTransform.exists()) firstTransform.delete()
				}
			}
		} catch (e: IOException) {
			Log.e(TAG, "FAILED TO CREATE TEMP FILES", e)
			Result.failure(ExportFileException())
		} catch (e: Exception) {
			if (e is CancellationException) throw e
			Result.failure(e)
		}
	}


	override fun cleanUp() {
		_transformer?.cancel()
		_transformer?.removeAllListeners()
		_transformer = null
	}
}