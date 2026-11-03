package com.eva.transcribe.data

import com.eva.database.entity.STTModelEntity
import com.eva.transcribe.data.models.ModelMetadata
import com.eva.transcribe.domain.models.STTModel
import com.eva.transcribe.domain.models.STTModelState
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal fun STTModel.toEntity(
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): STTModelEntity = STTModelEntity(
    modelId = modelId,
    name = name,
    displayName = displayName,
    locale = locale,
    engine = engine,
    externalModelURI = downloadURI,
    externalModelVersion = downloadedVersion,
    archiveSize = archiveSize,
    archiveFormat = archiveFormat,
    checksum = checksum,
    license = license ?: "",
    modelURI = localModelURI,
    status = when (state) {
        STTModelState.UN_AVAILABLE -> STTModelEntity.ModelStatus.NOT_READY
        STTModelState.DOWNLOADING -> STTModelEntity.ModelStatus.DOWNLOADING
        STTModelState.DOWNLOADED -> STTModelEntity.ModelStatus.DOWNLOADED
        STTModelState.UPDATE_AVAILABLE -> STTModelEntity.ModelStatus.UPDATE_AVAILABLE
    },
    updatedAt = Clock.System.now().toLocalDateTime(timeZone),
)

internal fun STTModelEntity.toModel(): STTModel = STTModel(
    modelId = modelId,
    name = name,
    displayName = displayName,
    locale = locale,
    engine = engine,
    downloadURI = externalModelURI,
    downloadedVersion = externalModelVersion,
    archiveSize = archiveSize,
    archiveFormat = archiveFormat,
    checksum = checksum,
    license = license,
    localModelURI = modelURI,
    state = when (status) {
        STTModelEntity.ModelStatus.NOT_READY -> STTModelState.UN_AVAILABLE
        STTModelEntity.ModelStatus.DOWNLOADING -> STTModelState.DOWNLOADING
        STTModelEntity.ModelStatus.DOWNLOADED -> STTModelState.DOWNLOADED
        STTModelEntity.ModelStatus.UPDATE_AVAILABLE -> STTModelState.UPDATE_AVAILABLE
    },
)

internal fun ModelMetadata.toEntity(
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): STTModelEntity = STTModelEntity(
    modelId = modelId,
    name = name,
    displayName = displayName,
    locale = locale,
    engine = engine,
    externalModelURI = modelUri,
    externalModelVersion = version,
    archiveSize = sizeBytes,
    archiveFormat = archiveFormat,
    checksum = checksum,
    license = license,
    updatedAt = Clock.System.now().toLocalDateTime(timeZone),
)
