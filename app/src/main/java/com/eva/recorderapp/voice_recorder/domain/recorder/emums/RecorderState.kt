package com.eva.recorderapp.voice_recorder.domain.recorder.emums

enum class RecorderState {
	IDLE,
	PREPARING,
	RECORDING,
	PAUSED,
	COMPLETED,
	CANCELLED;

	val isRecording: Boolean
		get() = this == RECORDING

	val canReadAmplitudes: Boolean
		get() = this in arrayOf(RECORDING, PAUSED, PREPARING)
}
