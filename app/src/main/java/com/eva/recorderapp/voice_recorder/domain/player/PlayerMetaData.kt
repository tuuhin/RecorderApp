package com.eva.recorderapp.voice_recorder.domain.player

data class PlayerMetaData(
	val playerState: PlayerState = PlayerState.IDLE,
	val playBackSpeed: PlayerPlayBackSpeed = PlayerPlayBackSpeed.NORMAL,
	val isRepeating: Boolean = false,
	val isMuted: Boolean = false,
) {
	val isPlaying: Boolean
		get() = playerState == PlayerState.PLAYING
}