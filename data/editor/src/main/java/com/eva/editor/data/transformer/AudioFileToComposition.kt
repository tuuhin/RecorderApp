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

private const val TAG = "AUDIO_COMPOSER"

@OptIn(UnstableApi::class)
internal fun AudioFileModel.toComposition(
	actions: AudioConfigToActionList,
	gap: Duration = Duration.ZERO,
): Composition {

	val composedConfigs = EditorComposer.applyLogicalEditSequence(duration, actions)
	val ranges = mutableListOf<ClosedRange<Duration>>()

	val editableItems = buildList {
		for (config in composedConfigs) {
			val startMs = (config.start.inWholeMilliseconds / 10) * 10
			val endMs = (config.end.inWholeMilliseconds / 10) * 10

			val duration = abs(endMs - startMs)

			// duration should not be empty ensuring its least one millisecond
			if (duration <= 0) continue

			val clippingConfig = MediaItem.ClippingConfiguration.Builder()
				.setStartPositionMs(startMs)
				.setEndPositionMs(endMs)
				.build()

			ranges.add(config.start..config.end)

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

	Log.d(TAG, "CLIPPING :${ranges.joinToString("|")}")

	val itemSequence = EditedMediaItemSequence.Builder().also { builder ->
		editableItems.forEachIndexed { idx, item ->
			builder.addItem(item)
			if (gap > Duration.ZERO && idx + 1 < editableItems.size)
				builder.addGap(gap.inWholeMicroseconds)
		}
	}.build()

	return Composition.Builder(itemSequence)
		.build()

}
