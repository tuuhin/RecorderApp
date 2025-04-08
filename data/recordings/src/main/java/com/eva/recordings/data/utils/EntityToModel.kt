package com.eva.recordings.data.utils

import com.eva.database.entity.RecordingsMetaDataEntity
import com.eva.database.entity.TrashFileEntity
import com.eva.recordings.domain.models.ExtraRecordingMetadataModel
import com.eva.recordings.domain.models.RecordedVoiceModel
import com.eva.recordings.domain.models.TrashRecordingModel
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days

internal fun RecordingsMetaDataEntity.toModel(): ExtraRecordingMetadataModel =
	ExtraRecordingMetadataModel(
		recordingId = recordingId,
		isFavourite = isFavourite,
		categoryId = categoryId
	)

internal fun RecordedVoiceModel.toMetadataEntity(): RecordingsMetaDataEntity =
	RecordingsMetaDataEntity(recordingId = id, isFavourite = isFavorite, categoryId = categoryId)

internal fun TrashRecordingModel.toEntity() = TrashFileEntity(
	id = id,
	title = title,
	displayName = displayName,
	mimeType = mimeType,
	dateAdded = recordedAt,
	expiresAt = expiresAt,
	file = fileUri
)

internal fun TrashFileEntity.toModel() = TrashRecordingModel(
	id = id,
	title = title,
	displayName = displayName,
	mimeType = mimeType,
	recordedAt = dateAdded,
	fileUri = file,
	expiresAt = expiresAt ?: Clock.System.now().plus(7.days)
		.toLocalDateTime(TimeZone.currentSystemDefault())
)

internal fun RecordedVoiceModel.toEntity(expires: LocalDateTime, fileUri: String) = TrashFileEntity(
	id = id,
	title = title,
	displayName = displayName,
	mimeType = mimeType,
	dateAdded = recordedAt,
	expiresAt = expires,
	file = fileUri
)