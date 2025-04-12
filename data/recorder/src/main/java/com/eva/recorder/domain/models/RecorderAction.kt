package com.eva.recorder.domain.models

sealed class RecorderAction(val action: String) {
	data object StartRecorderAction : RecorderAction("RECORDER_ACTION_START")
	data object ResumeRecorderAction : RecorderAction("RECORDER_ACTION_RESUME")
	data object PauseRecorderAction : RecorderAction("RECORDER_ACTION_PAUSE")
	data object StopRecorderAction : RecorderAction("RECORDER_ACTION_STOP")
	data object CancelRecorderAction : RecorderAction("RECORDER_ACTION_CANCEL")
	data object AddBookMarkAction : RecorderAction("RECORDER_ACTION_NEW_BOOKMARK")
}