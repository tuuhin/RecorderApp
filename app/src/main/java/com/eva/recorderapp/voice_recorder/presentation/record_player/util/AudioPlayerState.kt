package com.eva.recorderapp.voice_recorder.presentation.record_player.util

import com.eva.recorderapp.voice_recorder.domain.player.PlayerPlayBackSpeed
import com.eva.recorderapp.voice_recorder.domain.player.PlayerTrackData

data class AudioPlayerState(
	val trackData: PlayerTrackData = PlayerTrackData(),
	val isPlaying: Boolean = false,
	val playBackSpeed: PlayerPlayBackSpeed = PlayerPlayBackSpeed.NORMAL,
	val isRepeating: Boolean = false,
)
