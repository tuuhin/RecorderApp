package com.eva.editor.data

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.eva.editor.domain.AudioTrimmer
import com.eva.editor.domain.TransformationProgress
import com.eva.editor.domain.exceptions.MediaUnsupportedException
import com.eva.editor.domain.exceptions.TransformRunningException
import com.eva.editor.domain.exceptions.TransformerConfigException
import com.eva.recordings.domain.models.AudioFileModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "AUDIO_TRIMMER"

@UnstableApi
internal class AudioTrimmerImpl(private val context: Context) : AudioTrimmer,
	CoroutineScope by MainScope() {

	private val _isTransforming = MutableStateFlow(false)

	private var _transformer: Transformer? = null
	private var _resultsFile: File? = null

	private val _errors = MutableSharedFlow<Exception>()
	override val errorsFlow: SharedFlow<Exception>
		get() = _errors

	@OptIn(ExperimentalCoroutinesApi::class)
	override val progress: Flow<TransformationProgress>
		get() = _isTransforming.flatMapLatest(::updateTransformerProgress)
			.onStart { emit(TransformationProgress.Idle) }
			.distinctUntilChanged()


	private val _transformListener = object : Transformer.Listener {
		override fun onCompleted(composition: Composition, exportResult: ExportResult) {
			super.onCompleted(composition, exportResult)
			Log.d(TAG, "COMPOSITION COMPLETE")

			_isTransforming.update { false }
			_resultsFile?.let {
				Log.d(TAG, "FILE CREATED :${it.path} ${it.length()}")
			}
			// do something here with the content of the file.
		}

		override fun onError(
			composition: Composition,
			exportResult: ExportResult,
			exportException: ExportException
		) {
			super.onError(composition, exportResult, exportException)
			Log.d(TAG, "COMPOSITION FAILED")

			_isTransforming.update { false }
			// we don't want the temp file anymore
			_resultsFile?.delete()
			_resultsFile = null

			exportResult.exportException?.let {
				launch { _errors.emit(it) }
			}
		}
	}


	fun prepareTransformer(mimeType: String): Result<Unit> {
		val options = arrayOf(
			MimeTypes.AUDIO_AAC,
			MimeTypes.AUDIO_AMR_NB,
			MimeTypes.AUDIO_AMR_WB
		)

		val mimeType = options.find { it == mimeType } ?: MimeTypes.AUDIO_AAC

		return try {
			_transformer = Transformer.Builder(context)
				.setAudioMimeType(mimeType)
				.experimentalSetTrimOptimizationEnabled(true)
				.build()

			Log.d(TAG, "PREPARED TRANSFORMER WITH MIME TYPE:$mimeType AND TRIM:true")

			_transformer?.addListener(_transformListener)
			Result.success(Unit)
		} catch (_: IllegalStateException) {
			Result.failure(TransformerConfigException())
		} catch (e: Exception) {
			Result.failure(e)
		}
	}

	override fun trimAudioFile(model: AudioFileModel, clipConfig: AudioClipConfig): Result<Unit> {

		if (_transformer == null) prepareTransformer(model.mimeType)

		if (_isTransforming.value) {
			Log.d(TAG, "TRANSFORMATION RUNNING CANNOT EDIT..")
			return Result.failure(TransformRunningException())
		}

		val clippingConfig = MediaItem.ClippingConfiguration.Builder()
			.setStartPositionMs(clipConfig.start.inWholeMilliseconds)
			.setEndPositionMs(clipConfig.end.inWholeMilliseconds)
			.build()

		val mediaItem = MediaItem.Builder()
			.setUri(model.fileUri)
			.setClippingConfiguration(clippingConfig)
			.build()

		return try {

			val file = _resultsFile ?: run {
				_resultsFile = File.createTempFile("temp_", ".tmp", context.cacheDir)
				_resultsFile!!
			}

			Log.d(TAG, "BEGINNING TRANSFORMATION ")
			_transformer?.start(mediaItem, file.path)
			_isTransforming.update { true }
			Result.success(Unit)
		} catch (_: IllegalArgumentException) {
			Result.failure(MediaUnsupportedException())
		} catch (_: IllegalStateException) {
			Result.failure(TransformRunningException())
		} catch (e: Exception) {
			Result.failure(e)
		}
	}


	override fun cancelTransformation() {
		_transformer?.cancel()
	}

	override fun cleanUp() {
		_transformer?.removeListener(_transformListener)
		_transformer = null
		cancel()
	}

	private fun updateTransformerProgress(isTransformerRunning: Boolean) = flow {
		val holder = ProgressHolder()
		while (isTransformerRunning) {
			val state = withContext(Dispatchers.Main) { _transformer?.getProgress(holder) }
			when (state) {
				Transformer.PROGRESS_STATE_AVAILABLE -> {
					val progress = holder.progress
					emit(TransformationProgress.Progress(progress))
				}

				Transformer.PROGRESS_STATE_NOT_STARTED -> emit(TransformationProgress.Idle)
				Transformer.PROGRESS_STATE_UNAVAILABLE -> emit(TransformationProgress.UnAvailable)
				Transformer.PROGRESS_STATE_WAITING_FOR_AVAILABILITY ->
					emit(TransformationProgress.Waiting)

				else -> {}
			}
			delay(2.milliseconds)
		}
	}.flowOn(Dispatchers.IO)
}
