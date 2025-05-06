package com.eva.player.data.util

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

private const val TAG = "PLAYER_MEDIA_ITEM_TRANSITION"

fun Player.isMediaItemChange() = callbackFlow {

	val listener = object : Player.Listener {
		override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
			Log.d(TAG, "MEDIA ITEM TRANSITION REASON :$reason")
			launch { send(reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) }
		}
	}
	addListener(listener)

	awaitClose { removeListener(listener) }
}