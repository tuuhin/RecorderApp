package com.eva.player.data.util

import androidx.media3.common.Player
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

fun Player.computeIsPlayerPlaying(): Flow<Boolean> = callbackFlow {
	trySend(playbackState == Player.STATE_READY)

	val listener = object : Player.Listener {
		override fun onIsPlayingChanged(isPlaying: Boolean) {
			trySend(isPlaying)
		}
	}
	// adding the listener
	addListener(listener)
	// removing the listener
	awaitClose { removeListener(listener) }
}.distinctUntilChanged()
