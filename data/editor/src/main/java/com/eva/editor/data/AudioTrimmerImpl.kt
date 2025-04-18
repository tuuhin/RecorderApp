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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "AudioTrimmerTransformer"

@UnstableApi
internal class AudioTrimmerImpl(private val context: Context) : AudioTrimmer {

	private val isTransforming = MutableStateFlow(false)

	private var _transformer: Transformer? = null

	private val _tempFile: File
		get() = File.createTempFile("temp", "tmp", context.cacheDir)

	@OptIn(ExperimentalCoroutinesApi::class)
	override val transformationProgress: Flow<TransformationProgress>
		get() = isTransforming.flatMapLatest(::updateTransformerProgress)
			.distinctUntilChanged()


	private val _transformListener = object : Transformer.Listener {
		override fun onCompleted(composition: Composition, exportResult: ExportResult) {
			super.onCompleted(composition, exportResult)

			isTransforming.update { false }
			Log.d(TAG, "COMPOSITION COMPLETE")
			// do something here with the content of the file.

		}

		override fun onError(
			composition: Composition,
			exportResult: ExportResult,
			exportException: ExportException
		) {
			super.onError(composition, exportResult, exportException)

			isTransforming.update { false }
			Log.d(TAG, "COMPOSITION FAILED")
		}
	}


	override fun prepareTransformer() {
		_transformer = Transformer.Builder(context)
			.setAudioMimeType(MimeTypes.AUDIO_MP4)
			.experimentalSetTrimOptimizationEnabled(true)
			.build().apply {
				addListener(_transformListener)
			}
	}

	override fun trimAudioFile(uri: String, start: Duration, end: Duration) {

		if (isTransforming.value) {
			Log.d(TAG, "TRANSFORMATION RUNNING CANNOT EDIT..")
			return
		}

		val clippingConfig = MediaItem.ClippingConfiguration.Builder()
			.setStartPositionMs(start.inWholeMilliseconds)
			.setEndPositionMs(end.inWholeMilliseconds)
			.build()

		val mediaItem = MediaItem.Builder()
			.setUri(uri)
			.setClippingConfiguration(clippingConfig)
			.build()

		// run the transformer on media item for clipping
		if (_transformer != null) {
			_transformer?.start(mediaItem, _tempFile.path)
			isTransforming.update { true }
		}
	}


	override fun cancelTransformation() {
		_transformer?.cancel()
	}

	override fun cleanUp() {
		_transformer?.removeListener(_transformListener)
		_transformer = null
	}

	private fun updateTransformerProgress(isTransformerRunning: Boolean): Flow<TransformationProgress> {
		return flow {
			val progressHolder = ProgressHolder()
			while (isTransformerRunning) {
				val state = _transformer?.getProgress(progressHolder)
				when (state) {
					Transformer.PROGRESS_STATE_AVAILABLE -> {
						val progress = progressHolder.progress
						emit(TransformationProgress.Progress(progress))
					}

					Transformer.PROGRESS_STATE_NOT_STARTED -> emit(TransformationProgress.Idle)
					Transformer.PROGRESS_STATE_UNAVAILABLE -> emit(TransformationProgress.UnAvailable)
					Transformer.PROGRESS_STATE_WAITING_FOR_AVAILABILITY ->
						emit(TransformationProgress.Waiting)

					else -> {}
				}
				delay(100.milliseconds)
			}
		}.flowOn(Dispatchers.Main)
	}
}