package com.eva.player.data.service

import android.app.PendingIntent
import android.app.Service
import android.app.TaskStackBuilder
import android.content.Intent
import androidx.core.net.toUri
import com.eva.utils.IntentConstants
import com.eva.utils.IntentRequestCodes
import com.eva.utils.NavDeepLinks

internal fun Service.createBackStackIntent(audioId: Long): PendingIntent {
	val activityIntent = Intent().apply {
		setClassName(applicationContext, IntentConstants.MAIN_ACTIVITY)
	}

	return TaskStackBuilder.create(applicationContext).apply {
		// add the recorder
		addNextIntent(
			activityIntent.apply {
				data = NavDeepLinks.RECORDER_DESTINATION_PATTERN.toUri()
				action = Intent.ACTION_VIEW
			}
		)
		// the recordings
		addNextIntent(
			activityIntent.apply {
				data = NavDeepLinks.RECORDING_DESTINATION_PATTERN.toUri()
				action = Intent.ACTION_VIEW
			},
		)
		// then audio files
		if (audioId != -1L) {
			activityIntent.apply {
				data = NavDeepLinks.audioPlayerDestinationUri(audioId).toUri()
				action = Intent.ACTION_VIEW
			}
		}
	}.getPendingIntent(
		IntentRequestCodes.PLAYER_BACKSTACK_INTENT.code,
		PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
	)

}

internal fun Service.createPlayerIntent(audioId: Long): PendingIntent {
	return Intent().apply {
		setClassName(applicationContext, IntentConstants.MAIN_ACTIVITY)
		data = NavDeepLinks.audioPlayerDestinationUri(audioId).toUri()
		action = Intent.ACTION_VIEW
	}.let { intent ->
		PendingIntent.getActivity(
			applicationContext,
			IntentRequestCodes.PLAYER_NOTIFICATION_INTENT.code,
			intent,
			PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
		)
	}
}