package com.eva.editor.data.util

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import com.eva.editor.domain.model.AudioClipConfig
import com.eva.recordings.domain.models.AudioFileModel
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
internal fun AudioFileModel.toCropComposition(clipConfig: AudioClipConfig): Composition {
	val clippingConfig = MediaItem.ClippingConfiguration.Builder()
		.setStartPositionMs(clipConfig.start.inWholeMilliseconds)
		.setEndPositionMs(clipConfig.end.inWholeMilliseconds)
		.build()

	val mediaItem = MediaItem.Builder()
		.setUri(fileUri)
		.setClippingConfiguration(clippingConfig)
		.build()

	val editableItem = EditedMediaItem.Builder(mediaItem).build()

	val itemSequence = EditedMediaItemSequence.Builder(editableItem).build()

	return Composition.Builder(itemSequence).build()
}

@OptIn(UnstableApi::class)
internal fun AudioFileModel.toCutComposition(clipConfig: AudioClipConfig): Composition {
	val itemBuilder = MediaItem.Builder().setUri(fileUri)

	val startMediaItem = itemBuilder.setClippingConfiguration(
		MediaItem.ClippingConfiguration.Builder()
			.setStartPositionMs(0)
			.setEndPositionMs(clipConfig.start.inWholeMilliseconds)
			.build()
	).build()

	val endMediaItem = itemBuilder.setClippingConfiguration(
		MediaItem.ClippingConfiguration.Builder()
			.setStartPositionMs(clipConfig.end.inWholeMilliseconds)
			.setEndPositionMs(duration.inWholeMilliseconds)
			.build()
	).build()

	val editableItem1 = EditedMediaItem.Builder(startMediaItem)
		.setDurationUs(clipConfig.start.inWholeMicroseconds)
		.build()

	val editableItem2 = EditedMediaItem.Builder(endMediaItem)
		.setDurationUs(duration.inWholeMicroseconds - clipConfig.end.inWholeMicroseconds)
		.build()

	// create a proper sequence
	val mediaSequence = EditedMediaItemSequence.Builder()
		.addItem(editableItem1)
		.addGap(1.milliseconds.inWholeMicroseconds)
		.addItem(editableItem2)
		.setIsLooping(false)
		.build()

	return Composition.Builder(mediaSequence)
		.setTransmuxAudio(true)
		.build()
}