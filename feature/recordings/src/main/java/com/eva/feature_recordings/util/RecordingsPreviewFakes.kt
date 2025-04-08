package com.eva.feature_recordings.util

import com.eva.categories.domain.models.CategoryColor
import com.eva.categories.domain.models.CategoryType
import com.eva.categories.domain.models.RecordingCategoryModel
import com.eva.feature_recordings.bin.state.SelectableTrashRecordings
import com.eva.feature_recordings.bin.state.toSelectableRecordings
import com.eva.feature_recordings.recordings.state.SelectableRecordings
import com.eva.feature_recordings.recordings.state.toSelectableRecordings
import com.eva.recordings.domain.models.RecordedVoiceModel
import com.eva.recordings.domain.models.TrashRecordingModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.LocalDateTime
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

internal object RecordingsPreviewFakes {

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

	val FAKE_TRASH_RECORDINGS_MODEL = TrashRecordingModel(
		id = 0L,
		title = "TRASHED_001",
		displayName = "TRASHED",
		mimeType = "audio/mp3",
		expiresAt = LocalDateTime.now().toKotlinLocalDateTime(),
		recordedAt = LocalDateTime.now().toKotlinLocalDateTime(),
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
			.map { record -> record.copy(isSelected = Random.Default.nextBoolean()) }.toImmutableList()


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
		} + FAKE_CATEGORY_WITH_COLOR_AND_TYPE + RecordingCategoryModel.Companion.ALL_CATEGORY).reversed()
			.toImmutableList()

}