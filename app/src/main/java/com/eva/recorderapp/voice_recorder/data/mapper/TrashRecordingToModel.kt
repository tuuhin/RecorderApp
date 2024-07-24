package com.eva.recorderapp.voice_recorder.data.mapper

import com.eva.recorderapp.voice_recorder.data.database.TrashFileMetaDataEntity
import com.eva.recorderapp.voice_recorder.domain.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.domain.models.TrashRecordingModel
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days

fun TrashRecordingModel.toEntity() = TrashFileMetaDataEntity(
	id = id,
	title = title,
	displayName = displayName,
	mimeType = mimeType,
	dateAdded = recordedAt,
	expiresAt = expiresAt,
	file = fileUri
)

fun TrashFileMetaDataEntity.toModel() = TrashRecordingModel(
	id = id,
	title = title,
	displayName = displayName,
	mimeType = mimeType,
	recordedAt = dateAdded,
	fileUri = file,
	expiresAt = expiresAt ?: Clock.System.now().plus(7.days)
		.toLocalDateTime(TimeZone.currentSystemDefault())
)

fun RecordedVoiceModel.toEntity(expires: LocalDateTime, fileUri: String) = TrashFileMetaDataEntity(
	id = id,
	title = title,
	displayName = displayName,
	mimeType = mimeType,
	dateAdded = recordedAt,
	expiresAt = expires,
	file = fileUri
)