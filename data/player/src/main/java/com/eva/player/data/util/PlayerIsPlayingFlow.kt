package com.eva.player.data.util

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.eva.player.domain.model.PlayerPlayState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map

private const val TAG = "PLAYER_PLAYING_FLOW"

fun Player.computePlayerPlayState(): Flow<PlayerPlayState> = callbackFlow {

	var isSeeking = false
	// initially send a false
	trySend(PlayerPlayState.PAUSED)

	val listener = object : Player.Listener {

		override fun onPositionDiscontinuity(
			oldPosition: Player.PositionInfo,
			newPosition: Player.PositionInfo,
			reason: Int
		) {
			if (reason != Player.DISCONTINUITY_REASON_SEEK) return
			isSeeking = true
		}

		override fun onIsPlayingChanged(isPlaying: Boolean) {
			// skip is playing condition if the player is seeking
			if (isSeeking) return
			val state = if (isPlaying) PlayerPlayState.PLAYING else PlayerPlayState.PAUSED
			Log.d(TAG, "PLAYER IS PLAYING CHANGED :$state")
			trySend(state)
		}

		override fun onPlaybackStateChanged(playbackState: Int) {
			Player.STATE_BUFFERING
			val newState = when (playbackState) {
				Player.STATE_BUFFERING -> PlayerPlayState.BUFFERING
				Player.STATE_READY -> {
					// after seek player is again ready
					isSeeking = false
					if (playWhenReady) PlayerPlayState.PLAYING else PlayerPlayState.PAUSED
				}

				else -> PlayerPlayState.PAUSED
			}
			Log.d(TAG, "PLAY BACK STATE CHANGED :$newState")
			trySend(newState)
		}

		override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
			if (reason != Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) return
			val state = if (playWhenReady) PlayerPlayState.PLAYING else PlayerPlayState.PAUSED
			Log.d(TAG, "PLAY WHEN READY CHANGED :$state")
			trySend(state)
		}

		override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
			Log.d(TAG, "MEDIA ITEM TRANSITION")
			val reasons = arrayOf(
				Player.MEDIA_ITEM_TRANSITION_REASON_AUTO,
				Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED
			)
			// if its any of the reason set it to play when ready
			if (reason !in reasons) return
			val newState = when (playbackState) {
				Player.STATE_READY -> PlayerPlayState.PLAYING
				else -> PlayerPlayState.PAUSED
			}
			Log.d(TAG, "MEDIA ITEM TRANSITIONS")
			trySend(newState)
		}
	}
	// adding the listener
	addListener(listener)
	// removing the listener
	awaitClose { removeListener(listener) }
}.distinctUntilChanged()

fun Player.computeIsPlayerPlaying(): Flow<Boolean> = computePlayerPlayState()
	.filterNot { it == PlayerPlayState.BUFFERING }
	.map { it == PlayerPlayState.PLAYING }
	.distinctUntilChanged()
