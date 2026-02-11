package com.eva.transcribe.domain.models

enum class STTModelState {
	UN_AVAILABLE,
	DOWNLOADING,
	DOWNLOADED,
	UPDATE_AVAILABLE
}