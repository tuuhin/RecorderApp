package com.eva.player.domain.model

data class PlayerMetaData(
	val isPlaying: Boolean = false,
	val playerState: PlayerState = PlayerState.IDLE,
	val playBackSpeed: PlayerPlayBackSpeed = PlayerPlayBackSpeed.NORMAL,
	val isRepeating: Boolean = false,
	val isMuted: Boolean = false,
)