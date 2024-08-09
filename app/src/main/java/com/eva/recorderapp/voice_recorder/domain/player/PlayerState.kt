package com.eva.recorderapp.voice_recorder.domain.player

enum class PlayerState {
	IDLE,
	PLAYER_READY,
	PLAYING,
	PAUSED,
	COMPLETED;

	val canAdvertisePlayerCurrentPostion: Boolean
		get() = this in arrayOf(
			PlayerState.PLAYER_READY, PlayerState.PLAYING, PlayerState.PAUSED
		)
}