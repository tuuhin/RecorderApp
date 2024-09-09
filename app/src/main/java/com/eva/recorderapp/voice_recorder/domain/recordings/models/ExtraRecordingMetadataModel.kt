package com.eva.recorderapp.voice_recorder.domain.recordings.models

data class ExtraRecordingMetadataModel(
	val recordingId: Long,
	val isFavourite: Boolean,
	val categoryId: Long? = null,
) 