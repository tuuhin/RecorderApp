package com.eva.feature_widget.utils

import com.eva.feature_widget.recordings.RecordedModelsList
import com.eva.recordings.domain.models.RecordedVoiceModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.minutes

internal object WidgetPreviewFakes {

	val FAKE_VOICE_RECORDING_MODEL = RecordedVoiceModel(
		id = 0L,
		title = "Voice_001",
		displayName = "Voice_001.abc",
		duration = 5.minutes,
		sizeInBytes = 1024 * 20,
		modifiedAt = LocalDateTime.now().toKotlinLocalDateTime(),
		recordedAt = LocalDateTime.now().toKotlinLocalDateTime(),
		fileUri = "",
		mimeType = "audio/mp3"
	)

	val FAKE_VOICE_RECORDING_MODELS = RecordedModelsList(
		recordings = List(10) { FAKE_VOICE_RECORDING_MODEL }.toImmutableList()
	)

	val FAKE_VOICE_RECORDINGS_MODELS_WITH_FAVOURITES = RecordedModelsList(
		recordings = FAKE_VOICE_RECORDING_MODELS.recordings
			.mapIndexed { idx, model -> model.copy(isFavorite = idx % 2 == 0) })
}