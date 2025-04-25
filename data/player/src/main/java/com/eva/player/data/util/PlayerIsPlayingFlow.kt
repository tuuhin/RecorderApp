package com.eva.player.data.util

import androidx.media3.common.Player
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

fun Player.computeIsPlayerPlaying(): Flow<Boolean> = callbackFlow {

	// initially send a false
	trySend(false)

	val listener = object : Player.Listener {
		override fun onIsPlayingChanged(isPlaying: Boolean) {
			trySend(isPlaying)
		}

		override fun onPlaybackStateChanged(playbackState: Int) {
			trySend(playbackState == Player.STATE_READY)
		}
	}
	// adding the listener
	addListener(listener)
	// removing the listener
	awaitClose { removeListener(listener) }
}.distinctUntilChanged()
