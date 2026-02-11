package com.eva.transcribe.data

import com.eva.database.entity.STTModelEntity
import com.eva.transcribe.data.models.ModelMetadata
import com.eva.transcribe.domain.models.STTModel
import com.eva.transcribe.domain.models.STTModelState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

internal fun STTModel.toEntity(
	timeZone: TimeZone = TimeZone.currentSystemDefault()
): STTModelEntity = STTModelEntity(
	modelId = modelId,
	modelURI = localModelURI,
	locale = locale,
	externalModelURI = downloadURI,
	updatedAt = Clock.System.now().toLocalDateTime(timeZone),
	externalModelVersion = downloadedVersion,
	status = when (state) {
		STTModelState.UN_AVAILABLE -> STTModelEntity.ModelStatus.NOT_READY
		STTModelState.DOWNLOADING -> STTModelEntity.ModelStatus.DOWNLOADING
		STTModelState.DOWNLOADED -> STTModelEntity.ModelStatus.DOWNLOADED
		STTModelState.UPDATE_AVAILABLE -> STTModelEntity.ModelStatus.UPDATE_AVAILABLE
	},
)

internal fun STTModelEntity.toModel(): STTModel = STTModel(
	modelId = modelId,
	locale = locale,
	size = modelSize,
	localModelURI = modelURI,
	downloadURI = externalModelURI,
	downloadedVersion = externalModelVersion,
	state = when (this.status) {
		STTModelEntity.ModelStatus.NOT_READY -> STTModelState.UN_AVAILABLE
		STTModelEntity.ModelStatus.DOWNLOADING -> STTModelState.DOWNLOADING
		STTModelEntity.ModelStatus.DOWNLOADED -> STTModelState.DOWNLOADED
		STTModelEntity.ModelStatus.UPDATE_AVAILABLE -> STTModelState.UPDATE_AVAILABLE
	},
)

internal fun ModelMetadata.toEntity(
	timeZone: TimeZone = TimeZone.currentSystemDefault()
) = STTModelEntity(
	modelId = modelId,
	locale = locale,
	externalModelURI = modelUri,
	externalModelVersion = version,
	updatedAt = Clock.System.now().toLocalDateTime(timeZone),
)