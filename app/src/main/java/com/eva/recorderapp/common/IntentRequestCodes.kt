package com.eva.recorderapp.common

enum class IntentRequestCodes(val code: Int) {
	ACTIVITY_INTENT(100),
	START_VOICE_RECORDER(101),
	PAUSE_VOICE_RECORDER(102),
	RESUME_VOICE_RECORDER(103),
	STOP_VOICE_RECORDER(104),
}