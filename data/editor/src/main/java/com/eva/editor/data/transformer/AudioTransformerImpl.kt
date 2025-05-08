package com.eva.editor.data.transformer

import android.content.Context
import android.text.format.Formatter
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.Transformer
import com.eva.editor.domain.AudioConfigToActionList
import com.eva.editor.domain.AudioTransformer
import com.eva.editor.domain.TransformationProgress
import com.eva.editor.domain.exceptions.TransformRunningException
import com.eva.editor.domain.exceptions.TransformerInvalidException
import com.eva.editor.domain.exceptions.TransformerWrongMimeTypeException
import com.eva.recordings.domain.models.AudioFileModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "AUDIO_TRANSFORMER"

@UnstableApi
internal class AudioTransformerImpl(private val context: Context) : AudioTransformer {

	private val _isTransforming = MutableStateFlow(false)
	private var _transformer: Transformer? = null

	@OptIn(ExperimentalCoroutinesApi::class)
	override val progress: Flow<TransformationProgress>
		get() = _isTransforming.flatMapLatest {
			_transformer?.transformerProgress(it) ?: emptyFlow()
		}
			.onStart { emit(TransformationProgress.Idle) }
			.distinctUntilChanged()

	fun prepareTransformer(mimeType: String? = null): Result<Unit> {
		val transformerMimetypes = arrayOf(
			MimeTypes.AUDIO_AAC,
			MimeTypes.AUDIO_AMR_NB,
			MimeTypes.AUDIO_AMR_WB
		)

		val mimeType = transformerMimetypes.find { it == mimeType } ?: MimeTypes.AUDIO_AAC

		return try {
			_transformer = Transformer.Builder(context)
				.setAudioMimeType(mimeType)
				.experimentalSetTrimOptimizationEnabled(true)
				.build()

			Log.d(TAG, "PREPARED TRANSFORMER WITH MIME TYPE:$mimeType AND TRIM:true")
			Result.success(Unit)
		} catch (_: IllegalStateException) {
			Result.failure(TransformerWrongMimeTypeException())
		} catch (e: Exception) {
			Result.failure(e)
		}
	}

	override suspend fun transformAudio(model: AudioFileModel, actionsList: AudioConfigToActionList)
			: Result<String> {
		val transformerMimetype = when (model.mimeType) {
			MimeTypes.AUDIO_AMR -> MimeTypes.AUDIO_AMR_NB
			MimeTypes.AUDIO_AMR_NB -> MimeTypes.AUDIO_AMR_NB
			MimeTypes.AUDIO_AMR_WB -> MimeTypes.AUDIO_AMR_WB
			else -> MimeTypes.AUDIO_AAC
		}

		if (_transformer == null) prepareTransformer(transformerMimetype)

		if (_isTransforming.value) {
			Log.d(TAG, "TRANSFORMATION RUNNING CANNOT EDIT..")
			return Result.failure(TransformRunningException())
		}

		val seekAbleFormats = arrayOf(MimeTypes.AUDIO_MP4, MimeTypes.AUDIO_AAC)

		// set transforming to true
		_isTransforming.update { true }
		return try {
			if (model.mimeType in seekAbleFormats)
				createFileAndTransform(model, actionsList)
			else createFileAndTransformIntoAcc(model, actionsList)
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


	private suspend fun createFileAndTransform(
		model: AudioFileModel,
		actionsList: AudioConfigToActionList
	): Result<String> = coroutineScope {
		val file = File.createTempFile("temp_", ".tmp", context.cacheDir)
		try {
			// if not acc type
			val composition = model.toComposition(actionsList)
			val filepath = _transformer?.awaitResults(composition, file.path)
				?: return@coroutineScope Result.failure(TransformerWrongMimeTypeException())

			if (file.exists()) {
				val fileSize = Formatter.formatFileSize(context, file.length())
				Log.d(TAG, "FILE CREATED :${file} $fileSize")
			}

			Result.success(filepath)
		} catch (e: CancellationException) {
			// delete the file as this export was cancelled
			withContext(NonCancellable) { file.delete() }
			throw e
		} catch (e: ExportException) {
			// delete the file as this export failed
			withContext(NonCancellable) { file.delete() }
			Result.failure(e)
		}
	}

	private suspend fun createFileAndTransformIntoAcc(
		model: AudioFileModel,
		actionsList: AudioConfigToActionList
	): Result<String> = coroutineScope {
		val firstTransform =
			async { File.createTempFile("first_conversion", ".tmp", context.cacheDir) }
		val finalFile =
			async { File.createTempFile("final_conversion_", ".tmp", context.cacheDir) }
		try {
			// convert the file into acc
			val mediaItem = MediaItem.fromUri(model.fileUri)
			val filePathAfterAccConvert = _transformer?.buildUpon()
				?.setAudioMimeType(MimeTypes.AUDIO_AAC)
				?.build()
				?.awaitResults(mediaItem, firstTransform.await().path)
				?: return@coroutineScope Result.failure(TransformerInvalidException())

			Log.d(TAG, "CONVERTED TO ACC FORMAT")

			// prepare the composition
			val newModel = model.copy(fileUri = filePathAfterAccConvert)
			val composition = newModel.toComposition(configs = actionsList)

			val endFile = finalFile.await()
			// final transformation
			val filePathAfterTransformer = _transformer
				?.buildUpon()
				?.setAudioMimeType(MimeTypes.AUDIO_AAC)
				?.build()
				?.awaitResults(composition, endFile.path)
				?: return@coroutineScope Result.failure(TransformerWrongMimeTypeException())

			Log.d(TAG, "TRANSFORMATION COMPLETED")

			if (endFile.exists()) {
				val fileSize = Formatter.formatFileSize(context, endFile.length())
				Log.d(TAG, "FILE CREATED :${endFile.path} $fileSize")
			}

			Result.success(filePathAfterTransformer)
		} catch (e: CancellationException) {
			// delete the file as this export was cancelled
			withContext(NonCancellable) { finalFile.await().delete() }
			throw e
		} catch (e: ExportException) {
			e.printStackTrace()
			// delete the file as this export failed
			withContext(NonCancellable) { finalFile.await().delete() }
			Result.failure(e)
		} catch (e: Exception) {
			e.printStackTrace()
			Result.failure(e)
		} finally {
			// first transform should be deleted everytime
			withContext(NonCancellable) {
				firstTransform.await().delete()
			}
		}
	}


	override fun cleanUp() {
		_transformer?.removeAllListeners()
		_transformer = null
	}
}