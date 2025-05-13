package com.eva.editor.data.transformer

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine

private const val TAG = "TRANSFORMATION_RESULTS"

@OptIn(ExperimentalCoroutinesApi::class)
@UnstableApi
internal suspend fun Transformer.awaitResults(composition: Composition, outputUri: String): String =
	suspendCancellableCoroutine { continuation ->
		val listener = object : Transformer.Listener {
			override fun onCompleted(composition: Composition, exportResult: ExportResult) {
				Log.d(TAG, "COMPOSITION SUCCESSES")
				continuation.resume(outputUri, null)
				// work is done so remove the listener
				removeListener(this)
				Log.d(TAG, "REMOVING LISTENER")
			}

			override fun onError(
				composition: Composition,
				exportResult: ExportResult,
				exportException: ExportException
			) {
				val message = buildString {
					append("COMPOSITION FAILED : ")
					append("MESSAGE :${exportException.message}")
					append("CAUSE :${exportException.cause?.message}")
				}
				Log.e(TAG, message)
				// it's a cancellation
				continuation.cancel(exportResult.exportException)
			}
		}
		// add the listener
		addListener(listener)
		// start the composition
		start(composition, outputUri)

		continuation.invokeOnCancellation {
			removeListener(listener)
			cancel()
			Log.d(TAG, "COROUTINE WAS CANCELLED REMOVING LISTENER AND CANCELLING TRANSFORMATION")
		}
	}


@OptIn(ExperimentalCoroutinesApi::class)
@UnstableApi
suspend fun Transformer.awaitResults(mediaItem: MediaItem, outputUri: String): String {
	val editMediaItem = EditedMediaItem.Builder(mediaItem).build()
	val sequence = EditedMediaItemSequence.Builder(editMediaItem).build()
	val composition = Composition.Builder(sequence).build()
	return awaitResults(composition, outputUri)
}