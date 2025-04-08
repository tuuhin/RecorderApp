package com.eva.feature_player.util

import com.eva.bookmarks.domain.AudioBookmarkModel
import com.eva.feature_player.state.AudioPlayerState
import com.eva.player.domain.model.PlayerTrackData
import com.eva.recordings.domain.models.AudioFileModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.datetime.toKotlinLocalTime
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal object PlayerPreviewFakes {

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

	val FAKE_AUDIO_INFORMATION = AudioPlayerState(
		trackData = PlayerTrackData(current = 4.seconds, total = 10.seconds),
		isControllerSet = true
	)

	val FAKE_BOOKMARK_MODEL = AudioBookmarkModel(
		bookMarkId = 0L,
		text = "Android",
		timeStamp = LocalTime.now().toKotlinLocalTime(),
		recordingId = 0L
	)

	val FAKE_BOOKMARKS_LIST = List(20) {
		AudioBookmarkModel(
			bookMarkId = it.toLong(),
			text = "Hello world",
			timeStamp = kotlinx.datetime.LocalTime.Companion.fromSecondOfDay(400),
			recordingId = 0L
		)
	}.toPersistentList()

}