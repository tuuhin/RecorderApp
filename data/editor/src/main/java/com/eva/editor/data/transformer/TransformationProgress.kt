package com.eva.editor.data.transformer

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.eva.editor.domain.TransformationProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
internal fun Transformer.transformerProgress(
	isTransformerRunning: Boolean,
	uiDelay: Duration = 16.milliseconds
) = flow {
	val holder = ProgressHolder()
	while (isTransformerRunning && currentCoroutineContext().isActive) {
		// process the delay then check for issues
		delay(uiDelay)

		val transformerProgress = withContext(Dispatchers.Main) { getProgress(holder) }
		val progressState = when (transformerProgress) {
			Transformer.PROGRESS_STATE_AVAILABLE -> {
				val progress = holder.progress
				TransformationProgress.Progress(progress)
			}

			Transformer.PROGRESS_STATE_NOT_STARTED -> TransformationProgress.Idle
			Transformer.PROGRESS_STATE_UNAVAILABLE -> TransformationProgress.UnAvailable
			Transformer.PROGRESS_STATE_WAITING_FOR_AVAILABILITY -> TransformationProgress.Waiting

			else -> continue
		}
		emit(progressState)
	}
}