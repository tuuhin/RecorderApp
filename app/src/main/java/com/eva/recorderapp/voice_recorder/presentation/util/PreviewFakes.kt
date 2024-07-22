package com.eva.recorderapp.voice_recorder.presentation.util

import com.eva.recorderapp.voice_recorder.domain.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.toSelectableRecordings
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

object PreviewFakes {

	val PREVIEW_RECORDER_AMPLITUDES = List(80) { Random(it).nextFloat() }.toImmutableList()

	val FAKE_VOICE_RECORDING_MODEL = RecordedVoiceModel(
		id = 0L,
		title = "AUD_ANDROID",
		displayName = "AUD_ANDROID",
		duration = 5.minutes,
		sizeInBytes = 1024 * 20,
		modifiedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
		recordedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
		fileUri = ""
	)

	val FAKE_VOICE_RECORDING_MODELS = List(10) { FAKE_VOICE_RECORDING_MODEL }
		.toSelectableRecordings()
		.toImmutableList()

	val FAKE_VOICE_RECORDINGS_SELECTED = List(10) { FAKE_VOICE_RECORDING_MODEL }
		.toSelectableRecordings()
		.mapIndexed { idx, record -> record.copy(isSelected = if (idx % 2 == 0) true else false) }
		.toImmutableList()

}