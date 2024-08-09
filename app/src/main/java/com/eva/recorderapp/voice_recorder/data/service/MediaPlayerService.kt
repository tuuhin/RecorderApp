package com.eva.recorderapp.voice_recorder.data.service

import android.app.PendingIntent
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.os.bundleOf
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaConstants
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

	private var mediaSession: MediaSession? = null

	@Inject
	lateinit var player: Player

	@Inject
	lateinit var notification: MediaNotification.Provider

	@Inject
	lateinit var sessionCallback: MediaSession.Callback

	val listener = object : MediaSessionService.Listener {
		override fun onForegroundServiceStartNotAllowedException() {
			Log.e(TAG, "CANNOT START FOREGROUND SERVICE")
		}
	}

	override fun onCreate() {
		super.onCreate()

		mediaSession = createMediaSession()
		setMediaNotificationProvider(notification)
		Log.d(TAG, "MEDIA SESSION CONFIGURED AND NOTIFICATON SET")
	}

	override fun onBind(intent: Intent?): IBinder? {
		return super.onBind(intent)
	}

	override fun onTaskRemoved(rootIntent: Intent?) {
		Log.d(TAG, "TASK REMOVED IS CALLED")
		// if the player is playing or its in ready state keep the notification
		if (player.playWhenReady || player.isPlaying) {
			player.pause()
		} else {
			// stop the on-going foreground
			stopForeground(STOP_FOREGROUND_REMOVE)
			// if the playback is running stop that and then stop the service
			stopSelf()
		}
	}


	override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {

		val audioId = controllerInfo.connectionHints
			.getLong(PlayerConstants.PLAYER_AUDIO_FILE_ID_KEY, -1)

		return mediaSession?.apply {
			setListener(listener)
			if (audioId != -1L) {
				val pendingIntent = createPlayerIntent(audioId)
				setSessionActivity(pendingIntent)
			}
		}
	}


	override fun onDestroy() {
		mediaSession?.apply {
			// release the player
			player.release()
			// release the session
			release()
		}
		mediaSession = null
		Log.d(TAG, "PLAYER SERVICE DESTROYED")
		super.onDestroy()
	}

	private fun createMediaSession(): MediaSession {

		val extras = bundleOf(
			MediaConstants.EXTRAS_KEY_SLOT_RESERVATION_SEEK_TO_NEXT to true,
			MediaConstants.EXTRAS_KEY_SLOT_RESERVATION_SEEK_TO_PREV to true
		)

		return MediaSession.Builder(this, player)
			.setExtras(extras)
			.setCallback(sessionCallback)
			.build()
	}

	private fun createPlayerIntent(audioId: Long): PendingIntent {
		return Intent(applicationContext, MainActivity::class.java).apply {
			data = NavDeepLinks.audioPlayerDestinationUri(audioId)
		}.let { intent ->
			PendingIntent.getActivity(
				applicationContext,
				IntentRequestCodes.PLAYER_NOTIFICATION_INTENT.code,
				intent,
				PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
			)
		}
	}
}