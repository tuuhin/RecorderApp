package com.eva.recorderapp.voice_recorder.data.service

import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.os.bundleOf
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.data.player.AudioPlayerMediaCallBacks
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "PLAYER_SERVICE"

@AndroidEntryPoint
class MediaPlayerService : MediaSessionService() {

	private var mediaSession: MediaSession? = null

	@Inject
	lateinit var player: Player

	@UnstableApi
	val listener = object : MediaSessionService.Listener {
		override fun onForegroundServiceStartNotAllowedException() {
			Log.d(TAG, "CANNOT START FOREGROUND SERVICE")
		}
	}

	@OptIn(UnstableApi::class)
	override fun onCreate() {
		super.onCreate()
		mediaSession = createMediaSession()

		val notification = AudioPlayerNotification(applicationContext).apply {
			setSmallIcon(R.drawable.ic_record_player)
		}

		setMediaNotificationProvider(notification)
		Log.d(TAG, "MEDIA SESSION CONFIGURED AND NOTIFICATON SET")
	}

	override fun onTaskRemoved(rootIntent: Intent?) {
		super.onTaskRemoved(rootIntent)
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

	override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
		mediaSession

	@OptIn(UnstableApi::class)
	private fun createMediaSession(): MediaSession {

		val extras = bundleOf(
			MediaConstants.EXTRAS_KEY_SLOT_RESERVATION_SEEK_TO_NEXT to true,
			MediaConstants.EXTRAS_KEY_SLOT_RESERVATION_SEEK_TO_PREV to true
		)

		val callbacks = AudioPlayerMediaCallBacks(applicationContext)

		return MediaSession.Builder(this, player)
			.setExtras(extras)
			.setCallback(callbacks)
			.build().apply {
				setListener(listener)
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
}