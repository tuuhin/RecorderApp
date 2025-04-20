package com.eva.player.data.util

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.eva.player.domain.model.PlayerTrackData
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "PLAYER_TRACK_DATA_FLOW"

fun Player.computePlayerTrackData(): Flow<PlayerTrackData> = callbackFlow {

	// first emission
	launch {
		val trackData = this@computePlayerTrackData.toTrackData
		if (trackData.allPositiveAndFinite) send(trackData)
	}

	var job: Job? = null

	val listener = object : Player.Listener {

		override fun onPlaybackStateChanged(playbackState: Int) {
			if (playbackState == Player.STATE_READY) {
				val trackData = this@computePlayerTrackData.toTrackData
				if (trackData.allPositiveAndFinite) trySend(trackData)
			}
		}

		override fun onIsPlayingChanged(isPlaying: Boolean) {
			// cancel the old coroutine and thus while loop is cancelled
			job?.cancel()
			job = launch {
				while (isPlaying && isActive) {
					val trackData = this@computePlayerTrackData.toTrackData
					if (!trackData.allPositiveAndFinite) continue
					send(trackData)
					delay(50.milliseconds)
				}
			}
		}

		override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
			val trackData = this@computePlayerTrackData.toTrackData
			if (trackData.allPositiveAndFinite) trySend(trackData)
		}
	}

	// add the listener
	Log.d(TAG, "LISTENER ADDED")
	addListener(listener)

	//remove the listener
	awaitClose {
		job?.cancel()
		Log.d(TAG, "REMOVING LISTENER")
		removeListener(listener)
	}
}
	.filter { it.allPositiveAndFinite }
	.distinctUntilChanged()


