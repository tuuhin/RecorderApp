package com.eva.recorderapp.voice_recorder.data.recordings.utils

import com.eva.recorderapp.voice_recorder.data.recordings.database.entity.RecordingsMetaDataEntity
import com.eva.recorderapp.voice_recorder.domain.recordings.models.ExtraRecordingMetadataModel

fun RecordingsMetaDataEntity.toModel(): ExtraRecordingMetadataModel = ExtraRecordingMetadataModel(
	recordingId = recordingId,
	isFavourite = isFavourite,
	categoryId = categoryId
)