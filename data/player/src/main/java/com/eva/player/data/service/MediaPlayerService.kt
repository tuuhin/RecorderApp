package com.eva.player.data.service

import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
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

	private val listener = object : Listener {
		override fun onForegroundServiceStartNotAllowedException() {
			Log.e(TAG, "CANNOT START FOREGROUND SERVICE")
		}
	}

	override fun onCreate() {
		super.onCreate()

		setMediaNotificationProvider(notification)
		Log.d(TAG, "MEDIA SESSION CONFIGURED AND NOTIFICATION SET")
	}


	override fun onTaskRemoved(rootIntent: Intent?) {
		val player = mediaSession.player
		if (player.playWhenReady) {
			// Make sure the service is not in foreground.
			player.pause()
		}
		stopSelf()
	}


	override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
		Log.i(TAG, "SESSION SET")

		audioId = controllerInfo.connectionHints
			.getLong(PLAYER_AUDIO_FILE_ID_KEY, -1)

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


	companion object {
		const val PLAYER_AUDIO_FILE_ID_KEY = "PLAYER_AUDIO_ID"
	}
}