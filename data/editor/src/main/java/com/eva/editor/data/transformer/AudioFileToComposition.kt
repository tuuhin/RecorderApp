package com.eva.editor.data.transformer

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import com.eva.editor.domain.AudioConfigToActionList
import com.eva.editor.domain.EditorComposer
import com.eva.recordings.domain.models.AudioFileModel
import kotlin.math.abs
import kotlin.time.Duration

private const val TAG = "AUDIO_FILE_COMPOSER"

@OptIn(UnstableApi::class)
internal fun AudioFileModel.toComposition(
	configs: AudioConfigToActionList,
	gap: Duration = Duration.ZERO,
): Composition {

	val composedConfigs = EditorComposer.applyLogicalEditSequence(duration, configs)

	val editableItems = buildList {
		composedConfigs.forEach { config ->
			val startMs = (config.start.inWholeMilliseconds / 10) * 10
			val endMs = (config.end.inWholeMilliseconds / 10) * 10

			val duration = abs(endMs - startMs)

			if (duration >= 0L) {
				val clippingConfig = MediaItem.ClippingConfiguration.Builder()
					.setStartPositionMs(startMs)
					.setEndPositionMs(endMs)
					.build()

				Log.d(TAG, "CLIPPING APPLIED :$startMs->$endMs")

				val mediaItem = MediaItem.Builder()
					.setUri(fileUri)
					.setClippingConfiguration(clippingConfig)
					.build()

				val videoEffects = emptyList<Effect>()
				val audioEffect = listOf<AudioProcessor>()

				val editableItem = EditedMediaItem.Builder(mediaItem)
					.setEffects(Effects(audioEffect, videoEffects))
					.build()

				add(editableItem)
			}
		}
	}
	val itemSequenceBuilder = EditedMediaItemSequence.Builder()

	editableItems.forEach {
		itemSequenceBuilder.addItem(it)
		if (gap > Duration.ZERO)
			itemSequenceBuilder.addGap(gap.inWholeMicroseconds)
	}


	val itemSequence = itemSequenceBuilder.build()

	return Composition.Builder(itemSequence).build()

}
