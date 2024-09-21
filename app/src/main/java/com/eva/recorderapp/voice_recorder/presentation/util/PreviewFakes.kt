package com.eva.recorderapp.voice_recorder.presentation.util

import com.eva.recorderapp.voice_recorder.domain.categories.models.CategoryColor
import com.eva.recorderapp.voice_recorder.domain.categories.models.CategoryType
import com.eva.recorderapp.voice_recorder.domain.categories.models.RecordingCategoryModel
import com.eva.recorderapp.voice_recorder.domain.player.model.AudioFileModel
import com.eva.recorderapp.voice_recorder.domain.recorder.VoiceRecorder
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.domain.recordings.models.TrashRecordingModel
import com.eva.recorderapp.voice_recorder.presentation.categories.utils.toSelectableCategory
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.AudioPlayerInformation
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.PlayerGraphInfo
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.SelectableRecordings
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.SelectableTrashRecordings
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.toSelectableRecordings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import java.time.LocalDateTime as JLocalDateTime

object PreviewFakes {

	private val now: LocalDateTime
		get() = JLocalDateTime.now().toKotlinLocalDateTime()


	val PREVIEW_RECORDER_AMPLITUDES = List(100) { Random.nextFloat() }.toImmutableList()


	private val PREVIEW_RECORDER_AMPLITUDE_FLOAT_ARRAY_LARGE = List(150) {
		Random.nextFloat()
	}.mapIndexed { idx, amp ->
		val duration = VoiceRecorder.AMPS_READ_DELAY_RATE.times(idx)
		val time = LocalTime.fromMillisecondOfDay(duration.inWholeMilliseconds.toInt())
		time to amp
	}

	val PREVIEW_RECORDER_AMPLITUDES_FLOAT_ARRAY =
		PREVIEW_RECORDER_AMPLITUDE_FLOAT_ARRAY_LARGE.take(100)

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
		path = "this_is_a_path/file",
		channel = 1,
		size = 100L,
		mimeType = "This/that"
	)

	val FAKE_TRASH_RECORDINGS_MODEL = TrashRecordingModel(
		id = 0L,
		title = "TRASHED_001",
		displayName = "TRASHED",
		mimeType = "audio/mp3",
		expiresAt = now,
		recordedAt = now,
		fileUri = "",
	)

	val FAKE_TRASH_RECORDINGS_EMPTY = persistentListOf<SelectableTrashRecordings>()

	val FAKE_TRASH_RECORDINGS_MODELS =
		List(10) { FAKE_TRASH_RECORDINGS_MODEL }.toSelectableRecordings().toImmutableList()

	val FAKE_VOICE_RECORDINGS_EMPTY = persistentListOf<SelectableRecordings>()

	val FAKE_VOICE_RECORDING_MODELS =
		List(10) { FAKE_VOICE_RECORDING_MODEL }.toSelectableRecordings().toImmutableList()

	val FAKE_VOICE_RECORDINGS_SELECTED =
		List(10) { FAKE_VOICE_RECORDING_MODEL }.toSelectableRecordings()
			.map { record -> record.copy(isSelected = Random.nextBoolean()) }.toImmutableList()

	private val FAKE_RECORDING_CATEGORY = RecordingCategoryModel(
		id = 0L, name = "Something", createdAt = now
	).toSelectableCategory()

	val FAKE_RECORDING_CATEGORIES = List(10) { FAKE_RECORDING_CATEGORY }.toImmutableList()

	val FAKE_RECORDINGS_CATEGORIES_FEW_SELECTED =
		List(10) { FAKE_RECORDING_CATEGORY }.map { category -> category.copy(isSelected = Random.nextBoolean()) }
			.toImmutableList()

	val FAKE_CATEGORY_WITH_COLOR_AND_TYPE = RecordingCategoryModel(
		id = 0L,
		name = "Android",
		categoryType = CategoryType.CATEGORY_SONG,
		categoryColor = CategoryColor.COLOR_BLUE
	)

	val FAKE_CATEGORIES_WITH_ALL_OPTION: ImmutableList<RecordingCategoryModel>
		get() = (List(4) {
			RecordingCategoryModel(
				id = 0L,
				name = "Android",
				categoryType = CategoryType.entries.random(),
				categoryColor = CategoryColor.entries.random()
			)
		} + FAKE_CATEGORY_WITH_COLOR_AND_TYPE + RecordingCategoryModel.ALL_CATEGORY).reversed()
			.toImmutableList()


}