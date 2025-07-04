package com.eva.player.data.util

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

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

		override fun onPositionDiscontinuity(
			oldPosition: Player.PositionInfo,
			newPosition: Player.PositionInfo,
			reason: Int
		) {
			if (reason != Player.DISCONTINUITY_REASON_SEEK) return
			// on seek update the current position to the final seek position
			launch {
				val newPosDuration = newPosition.positionMs.milliseconds
				val trackData = this@computePlayerTrackData.toTrackData()
					.copy(current = newPosDuration)
				if (trackData.allPositiveAndFinite) send(trackData)
			}
		}

		override fun onIsPlayingChanged(isPlaying: Boolean) {
			// cancel the old coroutine
			job?.cancel()
			// if playing launch a new job to observe
			val canLoop = isPlaying && this@callbackFlow.isActive
			job = launch(Dispatchers.Default) {
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
			if (reason in arrayOf(
					Player.MEDIA_ITEM_TRANSITION_REASON_AUTO,
					Player.MEDIA_ITEM_TRANSITION_REASON_SEEK
				)
			) {
				launch {
					val trackData = this@computePlayerTrackData.toTrackData()
					if (trackData.allPositiveAndFinite) send(trackData)
				}
			}
			if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
				val itemDuration = mediaItem?.mediaMetadata?.durationMs?.milliseconds ?: return
				Log.d(TAG, "MEDIA ITEM TRANSITION NEW DURATION :$itemDuration")
				launch {
					val trackData = PlayerTrackData(0.seconds, itemDuration)
					if (trackData.allPositiveAndFinite) send(trackData)
				}
			}
		}

		override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
			val itemDuration = mediaMetadata.durationMs?.milliseconds ?: return
			Log.d(TAG, "MEDIA ITEM METADATA CHANGED NEW DURATION :$itemDuration")
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
}.filter { it.allPositiveAndFinite }
	.distinctUntilChanged()


