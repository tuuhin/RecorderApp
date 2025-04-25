package com.eva.player.data.util

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.eva.player.domain.model.PlayerTrackData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "PLAYER_TRACK_DATA_FLOW"

fun Player.computePlayerTrackData(): Flow<PlayerTrackData> = callbackFlow {

	// first emission
	launch {
		val trackData = this@computePlayerTrackData.toTrackData()
		if (trackData.allPositiveAndFinite) send(trackData)
	}

	var job: Job? = null

	val listener = object : Player.Listener {

		override fun onPlaybackStateChanged(playbackState: Int) {
			if (playbackState == Player.STATE_READY) {
				launch {
					val trackData = this@computePlayerTrackData.toTrackData()
					if (trackData.allPositiveAndFinite) send(trackData)
				}
			}
		}

		override fun onIsPlayingChanged(isPlaying: Boolean) {
			// cancel the old coroutine
			job?.cancel()
			// if playing launch a new job to observe
			val canLoop = isPlaying && this@callbackFlow.isActive
			job = launch {
				try {
					// advertise data if its active and canLoop
					while (canLoop && isActive) {
						delay(50.milliseconds)
						val trackData = this@computePlayerTrackData.toTrackData()
						if (!trackData.allPositiveAndFinite) continue

						ensureActive()
						send(trackData)
					}
				} catch (_: CancellationException) {
					Log.d(TAG, "CANNOT ADVERTISE DATA ANY MORE COROUTINE CANCELLED")
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
		}

		override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
			launch {
				val trackData = this@computePlayerTrackData.toTrackData()
				if (trackData.allPositiveAndFinite) send(trackData)
			}
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
}.flowOn(Dispatchers.Default)
	.filter { it.allPositiveAndFinite }
	.distinctUntilChanged()


