package com.eva.editor.data.transformer

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.eva.editor.domain.TransformationProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.microseconds

@OptIn(UnstableApi::class)
internal fun Transformer.transformerProgress(isTransformerRunning: Boolean) = flow {
	val holder = ProgressHolder()
	while (isTransformerRunning) {
		val state = withContext(Dispatchers.Main) { getProgress(holder) }
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
		delay(10.microseconds)
	}
}.flowOn(Dispatchers.IO)