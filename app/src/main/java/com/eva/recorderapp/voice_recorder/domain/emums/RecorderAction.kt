package com.eva.recorderapp.voice_recorder.domain.emums

enum class RecorderAction(val action: String) {
	START_RECORDER("START_RECORDER"),
	RESUME_RECORDER("RESUME_RECORDER"),
	PAUSE_RECORDER("PAUSE_RECORDER"),
	STOP_RECORDER("STOP_RECORDER"),
}