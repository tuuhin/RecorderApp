package com.eva.recordings.domain.models

data class ExtraRecordingMetadataModel(
	val recordingId: Long,
	val isFavourite: Boolean,
	val categoryId: Long? = null,
) 