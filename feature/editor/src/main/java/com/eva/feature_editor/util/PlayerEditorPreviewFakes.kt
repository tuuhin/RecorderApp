package com.eva.feature_editor.util

import com.eva.recordings.domain.models.AudioFileModel
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.minutes

internal object PlayerEditorPreviewFakes {

	val FAKE_AUDIO_MODEL = AudioFileModel(
		id = 0L,
		title = "Voice_001",
		displayName = "Voice_001.abc",
		duration = 5.minutes,
		fileUri = "",
		bitRateInKbps = 0f,
		lastModified = LocalDateTime.now().toKotlinLocalDateTime(),
		samplingRateKHz = 0f,
		path = "this_is_a_path/file",
		channel = 1,
		size = 100L,
		mimeType = "This/that",
		isFavourite = true
	)

}