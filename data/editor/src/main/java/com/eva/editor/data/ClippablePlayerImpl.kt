package com.eva.editor.data

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.eva.editor.domain.SimpleAudioPlayer
import com.eva.player.data.util.computeIsPlayerPlaying
import com.eva.player.data.util.computePlayerTrackData
import com.eva.player.domain.model.PlayerTrackData
import com.eva.recordings.domain.models.AudioFileModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration

private const val TAG = "SimpleAudioPlayer"

class ClippablePlayerImpl(val player: Player) : SimpleAudioPlayer {

	private val _lock = Mutex()

	override val isPlaying: Flow<Boolean>
		get() = player.computeIsPlayerPlaying()

	@OptIn(ExperimentalCoroutinesApi::class)
	override val trackInfoAsFlow: Flow<PlayerTrackData>
		get() = player.computePlayerTrackData()

	override fun onSeekDuration(duration: Duration) {
		val command = player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
		if (!command) {
			Log.w(TAG, "PLAYER SEEK IN MEDIA COMMAND NOT FOUND")
			return
		}
		val totalDuration = player.duration
		val changedDuration = duration.inWholeMilliseconds
		if (changedDuration <= totalDuration) {
			Log.d(TAG, "SEEK POSITION $duration")
			player.seekTo(duration.inWholeMilliseconds)
		}
	}

	override suspend fun preparePlayer(audio: AudioFileModel) {
		val command = player.isCommandAvailable(Player.COMMAND_SET_MEDIA_ITEM)
		if (!command) return

		return _lock.checkLockAndPerformOperation(
			action = {
				// add media item
				val mediaItem = MediaItem.fromUri(audio.fileUri)
				player.setMediaItem(mediaItem)

				if (player.playbackState == Player.STATE_IDLE) {
					player.prepare()
					Log.d(TAG, "PLAYER PREPARED AND READY TO PLAY AUDIO")
					player.playWhenReady = true
				}
			},
		)
	}

	override suspend fun trimMediaItem(start: Duration, end: Duration) {
		if (end <= start) {
			Log.d(TAG, "END IS LESSER THAN START NOT ALLOWED")
		}

		return _lock.checkLockAndPerformOperation(
			action = {
				val mediaItem = player.currentMediaItem ?: return@checkLockAndPerformOperation

				val clippingConfig = MediaItem.ClippingConfiguration.Builder()
					.setStartPositionMs(start.inWholeMilliseconds)
					.setEndPositionMs(end.inWholeMilliseconds)
					.build()

				val clippedMediaItem = mediaItem.buildUpon()
					.setClippingConfiguration(clippingConfig)
					.build()
				// set the new clipped media
				player.setMediaItem(clippedMediaItem)
			},
		)
	}

	override suspend fun pausePlayer() {
		val command = player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)
		if (!command) {
			Log.w(TAG, "PLAYER PLAY PAUSE COMMAND NOT FOUND")
			return
		}
		return _lock.checkLockAndPerformOperation(
			action = {
				player.pause()
				Log.d(TAG, "PLAYER PAUSED")
			},
		)
	}

	override suspend fun startOrResumePlayer() {
		val command = player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)
		if (!command) {
			Log.w(TAG, "PLAYER PLAY PAUSE COMMAND NOT FOUND")
			return
		}
		return _lock.checkLockAndPerformOperation(
			action = {
				player.play()
				Log.d(TAG, "PLAYER RESUMED")
			},
		)
	}

	override suspend fun stopPlayer() {
		return _lock.checkLockAndPerformOperation(
			action = {
				player.stop()
				Log.d(TAG, "PLAYER STOPPED AND RESET")
			},
		)
	}

	override fun cleanUp() {
		player.clearMediaItems()
	}

	private suspend inline fun Mutex.checkLockAndPerformOperation(
		action: () -> Unit,
		onError: (Exception) -> Unit = {},
	) {
		if (holdsLock(this)) {
			Log.d(TAG, "CANNOT PERFORM OPERATION")
			return
		}
		withLock {
			try {
				action()
			} catch (e: Exception) {
				e.printStackTrace()
				onError(e)
			}
		}
	}
}