package com.eva.editor.data

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.eva.editor.domain.SimpleAudioPlayer
import com.eva.player.domain.model.PlayerState
import com.eva.player.domain.model.PlayerTrackData
import com.eva.recordings.domain.models.AudioFileModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "SimpleAudioPlayer"

class ClippablePlayerImpl(val player: Player) : SimpleAudioPlayer {

	private val _isPlaying = MutableStateFlow(false)
	private val _playerState = MutableStateFlow(PlayerState.IDLE)

	private val lock = Mutex()

	private val _listener = object : Player.Listener {

		override fun onIsPlayingChanged(isPlaying: Boolean) {
			_isPlaying.update { isPlaying }
		}

		override fun onPlaybackStateChanged(playbackState: Int) {
			val newState = when (playbackState) {
				Player.STATE_IDLE -> PlayerState.IDLE
				Player.STATE_ENDED -> PlayerState.COMPLETED
				Player.STATE_READY -> PlayerState.PLAYER_READY
				else -> return
			}
			// only updates idle ready and ended
			_playerState.update { newState }
		}

		override fun onPlayerError(error: PlaybackException) {
			Log.e(TAG, error.message ?: "PLAYER_ERROR", error)
		}
	}

	override val isPlaying: StateFlow<Boolean>
		get() = _isPlaying

	@OptIn(ExperimentalCoroutinesApi::class)
	override val trackInfoAsFlow: Flow<PlayerTrackData>
		get() = _playerState.flatMapLatest(::computeMusicTrackInfo)

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

		return checkLockAndPerformOperation(
			lock = lock,
			action = {
				player.addListener(_listener)
				Log.d(TAG, "PLAYER LISTENER ADDED")

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

		return checkLockAndPerformOperation(
			lock = lock,
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
		return checkLockAndPerformOperation(
			lock = lock,
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
		return checkLockAndPerformOperation(
			lock = lock,
			action = {
				player.play()
				Log.d(TAG, "PLAYER RESUMED")
			},
		)
	}

	override suspend fun stopPlayer() {
		return checkLockAndPerformOperation(
			lock = lock,
			action = {
				player.stop()
				Log.d(TAG, "PLAYER STOPPED AND RESET")
			},
		)
	}

	override suspend fun cleanUp() {
		player.removeListener(_listener)
		player.clearMediaItems()
	}


	private fun computeMusicTrackInfo(state: PlayerState): Flow<PlayerTrackData> {
		return flow {
			Log.d(TAG, "CURRENT PLAYER STATE: $state")
			try {
				while (state.canAdvertiseCurrentPosition) {

					val current = player.currentPosition.milliseconds
					val total = player.duration.milliseconds

					if (current.isNegative() || total.isNegative())
						continue

					val trackData = PlayerTrackData(current = current, total = total)
					emit(trackData)
					delay(100.milliseconds)
				}
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}.filter { it.allPositiveAndFinite }
			.distinctUntilChanged()
	}

	private suspend inline fun checkLockAndPerformOperation(
		lock: Mutex,
		action: () -> Unit,
		onError: (Exception) -> Unit = {},
	) {
		if (lock.holdsLock(this)) {
			Log.d(TAG, "CANNOT PERFORM OPERATION")
			return
		}
		lock.lock(this)
		try {
			action()
		} catch (e: Exception) {
			e.printStackTrace()
			onError(e)
		} finally {
			lock.unlock(this)
		}
	}
}