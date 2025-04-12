package com.eva.utils

object NotificationConstants {

	const val RECORDER_NOTIFICATION_ID = 1
	const val RECORDER_NOTIFICATION_SECONDARY_ID = 2

	const val DELETE_WORKER_NOTIFICATION_ID = 3

	const val PLAYER_NOTIFICATION_ID = 4

	// Recorder channel
	const val RECORDER_CHANNEL_ID = "recorder_channel"
	const val RECORDER_CHANNEL_NAME = "Recorder Channel"
	const val RECORDER_CHANNEL_DESC =
		"Channel to show notifications regarding the current running recorder"

	// Player channel
	const val PLAYER_CHANNEL_ID = "player_channel"
	const val PLAYER_CHANNEL_NAME = "player_channel"
	const val PLAYER_CHANNEL_DESC = "Channel to add the media playback"

	// show recording channel
	const val RECORDING_CHANNEL_ID = "recordings_channel"
	const val RECORDING_CHANNEL_NAME = "Recordings"
	const val RECORDING_CHANNEL_DESC =
		"Notifications related to completion and cancellation of recordings"
}