package com.eva.player.data.util

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val TAG = "PLAYER_PLAYING_FLOW"

fun Player.computeIsPlayerPlaying(): Flow<Boolean> = callbackFlow {

	// initially send a false
	trySend(false)

	val listener = object : Player.Listener {

		override fun onIsPlayingChanged(isPlaying: Boolean) {
			Log.d(TAG, "PLAYER IS PLAYING CHANGED")
			launch { send(isPlaying) }
		}

		override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
			Log.d(TAG, "PLAYER WHEN READY CHANGED $reason $playWhenReady")
			launch { send(playWhenReady) }
		}

		override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
			Log.d(TAG, "MEDIA ITEM TRANSITION")
			val reasons = arrayOf(
				Player.MEDIA_ITEM_TRANSITION_REASON_AUTO,
				Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED
			)
			// if its any of the reason set it to play when ready
			if (reason in reasons) launch { send(playWhenReady) }
		}
	}
	// adding the listener
	addListener(listener)
	// removing the listener
	awaitClose { removeListener(listener) }
}.distinctUntilChanged()
