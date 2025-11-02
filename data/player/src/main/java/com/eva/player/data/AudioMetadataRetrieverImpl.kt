package com.eva.player.data

import android.content.Context
import androidx.concurrent.futures.await
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.MetadataRetriever
import androidx.media3.exoplayer.source.MediaSource
import com.eva.player.domain.AudioMetadataRetriever
import com.eva.recordings.domain.models.AudioFileModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.seconds

@UnstableApi
internal class AudioMetadataRetrieverImpl(
	private val context: Context,
	private val mediaSource: MediaSource.Factory
) : AudioMetadataRetriever {

	override suspend fun retrieveAudioDuration(model: AudioFileModel): Duration? {
		return try {
			val mediaItem = MediaItem.fromUri(model.fileUri)

			val metadata = MetadataRetriever.Builder(context, mediaItem)
				.setMediaSourceFactory(mediaSource)
				.build()
			// returns microseconds long
			val microSeconds = metadata.use { retriever -> retriever.retrieveDurationUs().await() }
			microSeconds.microseconds
		} catch (_: Exception) {
			0.seconds
		}
	}
}