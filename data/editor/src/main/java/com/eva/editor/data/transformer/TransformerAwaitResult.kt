package com.eva.editor.data.transformer

import android.util.Log
import androidx.annotation.MainThread
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.TransformationRequest
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val TAG = "TRANSFORMATION_RESULTS"

@UnstableApi
@MainThread
internal suspend fun Transformer.awaitResults(
	composition: Composition,
	outputUri: String
): String = suspendCancellableCoroutine { continuation ->
	val listener = object : Transformer.Listener {
		override fun onCompleted(composition: Composition, exportResult: ExportResult) {
			Log.d(TAG, "COMPOSITION SUCCESSES")
			// work is done so remove the listener
			if (continuation.isActive) continuation.resume(outputUri)

			removeListener(this)
			Log.d(TAG, "REMOVING LISTENER")
		}

		override fun onError(
			composition: Composition,
			exportResult: ExportResult,
			exportException: ExportException,
		) {
			Log.e(TAG, "COMPOSITION FAILED", exportException)
			// it's a cancellation
			if (continuation.isActive) continuation.cancel(exportException)
			removeListener(this)
			Log.d(TAG, "REMOVING LISTENER")
		}

		override fun onFallbackApplied(
			composition: Composition,
			originalTransformationRequest: TransformationRequest,
			fallbackTransformationRequest: TransformationRequest
		) {
			Log.d(TAG, "CANNOT APPLY TRANSFORMATION")
			Log.i(
				TAG,
				"ORIGINAL :${originalTransformationRequest.audioMimeType} FALLBACK :${fallbackTransformationRequest.audioMimeType}"
			)
		}
	}
	// add the listener
	addListener(listener)
	// start the composition
	try {
		start(composition, outputUri)
		Log.d(TAG, "STARTING TRANSFORMATION")
	} catch (_: IllegalStateException) {
		Log.d(TAG, "EXPORT RUNNING NEED TO WAIT")
		removeListener(listener)
		cancel()
		return@suspendCancellableCoroutine
	}

	continuation.invokeOnCancellation {
		Log.d(TAG, "COROUTINE WAS CANCELLED")
		try {
			removeListener(listener)
			cancel()
		} catch (_: Exception) {
		}
	}
}


@UnstableApi
suspend fun Transformer.awaitResults(mediaItem: MediaItem, outputUri: String): String {
	val editMediaItem = EditedMediaItem.Builder(mediaItem).build()
	val sequence = EditedMediaItemSequence.Builder(editMediaItem).build()
	val composition = Composition.Builder(sequence).build()
	return awaitResults(composition, outputUri)
}