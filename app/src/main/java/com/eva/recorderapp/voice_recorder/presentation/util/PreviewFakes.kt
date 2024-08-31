package com.eva.recorderapp.voice_recorder.presentation.util

import com.eva.recorderapp.voice_recorder.domain.player.model.AudioFileModel
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.domain.recordings.models.TrashRecordingModel
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.AudioPlayerInformation
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.PlayerGraphInfo
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.SelectableRecordings
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.SelectableTrashRecordings
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.toSelectableRecordings
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

object PreviewFakes {

	private val now: LocalDateTime
		get() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

	private val nowTime: LocalTime
		get() = now.time

	val PREVIEW_RECORDER_AMPLITUDES = List(80) { Random.nextFloat() }.toImmutableList()

	val FAKE_AUDIO_INFORMATION =
		AudioPlayerInformation(waveforms = PlayerGraphInfo(waves = PREVIEW_RECORDER_AMPLITUDES))

	val FAKE_VOICE_RECORDING_MODEL = RecordedVoiceModel(
		id = 0L,
		title = "Voice_001",
		displayName = "Voice_001.abc",
		duration = 5.minutes,
		sizeInBytes = 1024 * 20,
		modifiedAt = now,
		recordedAt = now,
		fileUri = "",
		mimeType = "audio/mp3"
	)

	val FAKE_AUDIO_MODEL = AudioFileModel(
		id = 0L,
		title = "Voice_001",
		displayName = "Voice_001.abc",
		duration = 5.minutes,
		fileUri = "",
		bitRateInKbps = 0f,
		lastModified = now,
		samplingRatekHz = 0f,
		path = "Somepath/file",
		channel = 1,
		size = 100L, mimeType = "This/that"
	)

	val FAKE_TRASH_RECORDINGS_MODEL = TrashRecordingModel(
		id = 0L,
		title = "TRAHSED_001",
		displayName = "TRASHED",
		mimeType = "audio/mp3",
		expiresAt = now,
		recordedAt = now,
		fileUri = "",
	)

	val FAKE_TRASH_RECORDINGS_EMPTY = persistentListOf<SelectableTrashRecordings>()

	val FAKE_TRASH_RECORDINGS_MODELS = List(10) { FAKE_TRASH_RECORDINGS_MODEL }
		.toSelectableRecordings().toImmutableList()

	val FAKE_VOICE_RECORDINGS_EMPTY = persistentListOf<SelectableRecordings>()

	val FAKE_VOICE_RECORDING_MODELS = List(10) { FAKE_VOICE_RECORDING_MODEL }
		.toSelectableRecordings()
		.toImmutableList()

	val FAKE_VOICE_RECORDINGS_SELECTED = List(10) { FAKE_VOICE_RECORDING_MODEL }
		.toSelectableRecordings()
		.mapIndexed { idx, record -> record.copy(isSelected = if (idx % 2 == 0) true else false) }
		.toImmutableList()

}