package com.eva.recorderapp.voice_recorder.presentation.record_player.util

import com.eva.recorderapp.voice_recorder.domain.player.PlayerPlayBackSpeed
import com.eva.recorderapp.voice_recorder.domain.player.PlayerState
import com.eva.recorderapp.voice_recorder.domain.player.PlayerTrackData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class AudioPlayerInformation(
	val trackData: PlayerTrackData = PlayerTrackData(),
	val playerState: PlayerState = PlayerState.IDLE,
	val playBackSpeed: PlayerPlayBackSpeed = PlayerPlayBackSpeed.NORMAL,
	val isRepeating: Boolean = false,
	val sampling: ImmutableList<Float> = persistentListOf()
) {
	val isPlaying: Boolean
		get() = playerState == PlayerState.PLAYING
}
