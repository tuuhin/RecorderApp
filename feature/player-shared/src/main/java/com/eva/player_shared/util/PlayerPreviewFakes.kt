package com.eva.player_shared.util

import com.eva.player.domain.model.PlayerTrackData
import com.eva.recordings.domain.models.AudioFileModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.LocalDateTime
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object PlayerPreviewFakes {

	val PREVIEW_RECORDER_AMPLITUDES = List(100) { Random.Default.nextFloat() }.toImmutableList()

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

	val FAKE_TRACK_DATA = PlayerTrackData(current = 4.seconds, total = 10.seconds)
}