package com.eva.recorderapp.voice_recorder.data.service

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.eva.recorderapp.MainActivity
import com.eva.recorderapp.common.IntentRequestCodes
import com.eva.recorderapp.common.PlayerConstants
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavDeepLinks
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "PLAYER_SERVICE"

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class MediaPlayerService : MediaSessionService() {

	private var audioId: Long = -1

	@Inject
	lateinit var mediaSession: MediaSession

	@Inject
	lateinit var notification: MediaNotification.Provider

	private val listener = object : MediaSessionService.Listener {
		override fun onForegroundServiceStartNotAllowedException() {
			Log.e(TAG, "CANNOT START FOREGROUND SERVICE")
		}
	}

	override fun onCreate() {
		super.onCreate()

		setMediaNotificationProvider(notification)
		Log.d(TAG, "MEDIA SESSION CONFIGURED AND NOTIFICATON SET")
	}


	override fun onTaskRemoved(rootIntent: Intent?) {
		val player = mediaSession.player
		if (player.playWhenReady) {
			// Make sure the service is not in foreground.
			player.pause()
		}
		stopSelf()
	}


	override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
		Log.i(TAG, "SESSION SET")

		audioId = controllerInfo.connectionHints
			.getLong(PlayerConstants.PLAYER_AUDIO_FILE_ID_KEY, -1)

		return mediaSession.apply {
			setListener(listener)
			if (audioId != -1L) {
				val pendingIntent = createPlayerIntent(audioId)
				setSessionActivity(pendingIntent)
			}
		}
	}


	override fun onDestroy() {
		val backStackEntry = createBackStackIntent(audioId)

		mediaSession.apply {
			setSessionActivity(backStackEntry)
			// release the player
			player.release()
			// release the session
			release()
		}

		Log.d(TAG, "REMOVED SESSION LISTENER")
		clearListener()
		audioId = -1

		Log.d(TAG, "PLAYER SERVICE DESTROYED")
		super.onDestroy()
	}


	private fun createPlayerIntent(audioId: Long): PendingIntent {
		return Intent(applicationContext, MainActivity::class.java).apply {
			data = NavDeepLinks.audioPlayerDestinationUri(audioId)
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
}

private fun MediaPlayerService.createBackStackIntent(audioId: Long): PendingIntent {
	val activityIntent = Intent(applicationContext, MainActivity::class.java)

	return TaskStackBuilder.create(applicationContext).apply {
		// add the recorder
		addNextIntent(
			activityIntent.apply {
				data = NavDeepLinks.recorderDestinationUri
				action = Intent.ACTION_VIEW
			}
		)
		// the recordings
		addNextIntent(
			activityIntent.apply {
				data = NavDeepLinks.recordingsDestinationUri
				action = Intent.ACTION_VIEW
			},
		)
		// then audio files
		if (audioId != -1L) {
			activityIntent.apply {
				data = NavDeepLinks.audioPlayerDestinationUri(audioId)
				action = Intent.ACTION_VIEW
			}
		}
	}.getPendingIntent(
		IntentRequestCodes.PLAYER_BACKSTACK_INTENT.code,
		PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
	)
}