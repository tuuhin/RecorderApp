package com.eva.recorderapp.voice_recorder.domain.player

data class PlayerMetaData(
	val playerState: PlayerState = PlayerState.IDLE,
	val playBackSpeed: PlayerPlayBackSpeed = PlayerPlayBackSpeed.NORMAL,
	val isRepeating: Boolean = false,
)