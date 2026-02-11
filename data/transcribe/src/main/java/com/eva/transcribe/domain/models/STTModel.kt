package com.eva.transcribe.domain.models

data class STTModel(
	val modelId: String,
	val locale: String,
	val downloadURI: String,
	val downloadedVersion: String,
	val size: Long? = null,
	val state: STTModelState = STTModelState.UN_AVAILABLE,
	val localModelURI: String? = null,
)
