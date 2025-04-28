package com.eva.player.data

import android.util.Log
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import com.eva.player.domain.model.PlayerMetaData
import com.eva.player.domain.model.PlayerPlayBackSpeed
import com.eva.player.domain.model.PlayerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

private const val TAG = "AUDIO_PLAYER_LISTENER"

internal class AudioFilePlayerListener(private val player: Player) : Player.Listener {

	private val _playerState = MutableStateFlow(PlayerState.IDLE)
	private val _playBackSpeed = MutableStateFlow(PlayerPlayBackSpeed.NORMAL)
	private val _isLooping = MutableStateFlow(false)
	private val _isStreamMuted = MutableStateFlow(false)
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
			else -> return
		}
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

	override fun onVolumeChanged(volume: Float) {
		super.onVolumeChanged(volume)
		Log.d(TAG, "DEVICE VOLUME CHANGED")
		_isStreamMuted.update { volume == 0f }
	}


	override fun onPlayerError(error: PlaybackException) {
		// need to check for exceptions
		Log.e(TAG, error.message ?: "PLAYER_ERROR", error)
	}

	val playerMetaDataFlow: Flow<PlayerMetaData>
		get() = combine(
			_isPlaying,
			_playerState,
			_isLooping,
			_playBackSpeed,
			_isStreamMuted
		) { playing, state, repeat, speed, muted ->
			PlayerMetaData(
				isPlaying = playing,
				playerState = state,
				isRepeating = repeat,
				playBackSpeed = speed,
				isMuted = muted
			)
		}

	fun updateStateFromCurrentPlayerConfig() {
		Log.d(TAG, "UPDATING PLAYER CONFIG")
		// update the repeat mode
		_isLooping.update { player.repeatMode == Player.REPEAT_MODE_ONE }
		// update the playback speed
		_playBackSpeed.update { speed ->
			val paramsSpeed = player.playbackParameters.speed
			PlayerPlayBackSpeed.fromInt(paramsSpeed) ?: speed
		}
		// update the player state
		_playerState.update { oldState ->
			when (player.playbackState) {
				Player.STATE_IDLE -> PlayerState.IDLE
				Player.STATE_ENDED -> PlayerState.COMPLETED
				Player.STATE_READY -> PlayerState.PLAYER_READY
				else -> return@update oldState
			}
		}
		// update the playing state
		_isPlaying.update { player.isPlaying }
		// update volume
		_isStreamMuted.update { player.volume == 0f }
	}
}