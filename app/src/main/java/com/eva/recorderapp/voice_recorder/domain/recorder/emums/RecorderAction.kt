package com.eva.recorderapp.voice_recorder.domain.recorder.emums

sealed class RecorderAction(val action: String) {
	data object StartRecorderAction : RecorderAction("START_RECORDER")
	data object ResumeRecorderAction : RecorderAction("RESUME_RECORDER")
	data object PauseRecorderAction : RecorderAction("PAUSE_RECORDER")
	data object StopRecorderAction : RecorderAction("STOP_RECORDER")
	data object CancelRecorderAction : RecorderAction("CANCEL_RECORDER")
	data object AddBookMarkAction : RecorderAction("ADD_BOOKMARK")
}