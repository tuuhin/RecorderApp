package com.eva.utils

object IntentConstants {
	const val APPLICATION_NAME = "com.eva.recorderapp"
	const val MAIN_ACTIVITY = "com.eva.recorderapp.MainActivity"
	const val RECORDER_WIDGET_RECEIVER = "com.eva.feature_widget.receivers.RecorderWidgetReceiver"
	const val RECORDINGS_WIDGET_RECEIVER =
		"com.eva.feature_widget.receivers.RecordingsWidgetReceiver"

	const val ACTION_UPDATE_WIDGET = "$APPLICATION_NAME.update_widget"
	const val ACTION_RESET_RECORDING_WIDGET = "$APPLICATION_NAME.reset_recordings_widget"

	const val EXTRAS_RECORDER_TIME = "$APPLICATION_NAME.extras_recorder_time"
	const val EXTRAS_RECORDER_STATE = "$APPLICATION_NAME.extras_recorder_state"

}