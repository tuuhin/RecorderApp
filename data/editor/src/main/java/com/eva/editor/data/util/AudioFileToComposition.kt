package com.eva.editor.data.util

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import com.eva.editor.domain.AudioConfigToActionList
import com.eva.editor.domain.EditorComposer
import com.eva.recordings.domain.models.AudioFileModel
import kotlin.math.abs
import kotlin.time.Duration

@OptIn(UnstableApi::class)
internal fun AudioFileModel.toComposition(
	configs: AudioConfigToActionList,
	gap: Duration = Duration.ZERO
): Composition {
	val composedConfigs = EditorComposer.applyLogicalEditSequence(duration, configs)

	val message = buildString {
		composedConfigs.forEach { config ->
			append("[${config.start} --> ${config.end}]  ")
		}
	}

	Log.d("COMPOSITION", message)

	val editableItems = buildList {
		composedConfigs.forEach { config ->
			val startMs = config.start.inWholeMilliseconds
			val endMs = config.end.inWholeMilliseconds

			val duration = abs(endMs - startMs)

			if (duration >= 0L) {
				val clippingConfig = MediaItem.ClippingConfiguration.Builder()
					.setStartPositionMs(startMs)
					.setEndPositionMs(endMs)
					.build()

				val mediaItem = MediaItem.Builder()
					.setUri(fileUri)
					.setClippingConfiguration(clippingConfig)
					.build()

				val editableItem = EditedMediaItem.Builder(mediaItem)
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

	return Composition.Builder(itemSequence)
		.build()
}
