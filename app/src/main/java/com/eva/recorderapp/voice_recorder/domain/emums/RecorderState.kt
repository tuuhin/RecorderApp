package com.eva.recorderapp.voice_recorder.domain.emums

enum class RecorderState {
	IDLE,
	PREPARING,
	RECORDING,
	PAUSED,
	COMPLETED,
	CANCELLED
}