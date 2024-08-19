package com.eva.recorderapp.voice_recorder.data.player

import android.util.Log
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import com.eva.recorderapp.voice_recorder.domain.player.PlayerMetaData
import com.eva.recorderapp.voice_recorder.domain.player.PlayerPlayBackSpeed
import com.eva.recorderapp.voice_recorder.domain.player.PlayerState
import com.eva.recorderapp.voice_recorder.domain.player.PlayerTrackData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "AUDIO_PLAYER_LISTENER"

class AudioFilePlayerListener(
	private val player: Player
) : Player.Listener {

	private val _playerState = MutableStateFlow(PlayerState.IDLE)
	private val _playBackSpeed = MutableStateFlow(PlayerPlayBackSpeed.NORMAL)
	private val _isLooping = MutableStateFlow(false)
	private val _isDeviceMuted = MutableStateFlow(false)
	private val _isPlaying = MutableStateFlow(false)

	override fun onIsPlayingChanged(isPlaying: Boolean) {
		// only updates to play or pause mode
		_isPlaying.update { isPlaying }
		Log.d(TAG, "PLAYER IS PLAYING :$isPlaying")
	}

	override fun onPlaybackStateChanged(playbackState: Int) {
		val newState = when (playbackState) {
			Player.STATE_IDLE -> PlayerState.IDLE
			Player.STATE_ENDED -> PlayerState.COMPLETED
			Player.STATE_READY -> PlayerState.PLAYER_READY
			else -> null
		} ?: return
		// only updates idle ready and ended
		_playerState.update { newState }
		Log.d(TAG, "PLAYER STATE :$newState")
	}

	override fun onRepeatModeChanged(repeatMode: Int) {
		val isLooping = repeatMode == Player.REPEAT_MODE_ONE
		// is current one looping
		_isLooping.update { isLooping }
		Log.d(TAG, "PLAYER REPEATING :$isLooping")
	}

	override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
		val playerSpeed = playbackParameters.speed
		val speed = PlayerPlayBackSpeed.fromInt(playerSpeed) ?: return
		// updates the player speed
		_playBackSpeed.update { speed }
		Log.d(TAG, "PLAYER SPEED $playerSpeed")
	}


	override fun onPlayerError(error: PlaybackException) {
		// need to check for exceptions
		Log.e(TAG, error.message ?: "PLAYER_ERROR", error)
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	val trackInfoAsFlow: Flow<PlayerTrackData>
		get() = _playerState.flatMapLatest(::computeMusicTrackInfo)

	val playerMetaDataFlow: Flow<PlayerMetaData>
		get() = combine(
			_isPlaying,
			_playerState,
			_isLooping,
			_playBackSpeed,
			_isDeviceMuted
		) { playing, state, repeat, speed, muted ->
			PlayerMetaData(
				isPlaying = playing,
				playerState = state,
				isRepeating = repeat,
				playBackSpeed = speed,
				isMuted = muted
			)
		}

	fun computeMusicTrackInfo(state: PlayerState): Flow<PlayerTrackData> {
		return flow {
			Log.d(TAG, "CURRENT PLAYER STATE: $state")

			// If the player can advertise postions ie, its ready or play or paused
			// then continue the loop
			while (state.canAdvertiseCurrentPosition) {
				val trackData = PlayerTrackData(
					current = player.currentPosition.milliseconds,
					total = player.duration.milliseconds,
				)
				emit(trackData)
				delay(100.milliseconds)
			}
		}.distinctUntilChanged()
	}


	fun updateStateFromCurrentPlayerConfig() {
		// update the repeat mode
		_isLooping.update { player.repeatMode == Player.REPEAT_MODE_ONE }
		// update the playback speed
		_playBackSpeed.update {
			val paramsSpeed = player.playbackParameters.speed
			PlayerPlayBackSpeed.fromInt(paramsSpeed) ?: it
		}
		// update the player state
		_playerState.update {
			when {
				player.playbackState == Player.STATE_IDLE -> PlayerState.IDLE
				player.playbackState == Player.STATE_ENDED -> PlayerState.COMPLETED
				player.playbackState == Player.STATE_READY -> PlayerState.PLAYER_READY
				else -> return@update it
			}
		}
		// update the playing state
		_isPlaying.update { player.isPlaying }
	}
}