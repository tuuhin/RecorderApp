package com.eva.recorderapp.common

enum class IntentRequestCodes(val code: Int) {
	// Deep links pending intent
	ACTIVITY_INTENT_RECORDINGS(99),
	ACTIVITY_INTENT_RECORDER(100),

	// Recorder Actions
	START_VOICE_RECORDER(101),
	PAUSE_VOICE_RECORDER(102),
	RESUME_VOICE_RECORDER(103),
	STOP_VOICE_RECORDER(104),
	CANCEL_VOICE_RECORDER(105),

	// Player notification content intent
	PLAYER_NOTIFICATION_INTENT(200)
}