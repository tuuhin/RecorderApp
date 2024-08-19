package com.eva.recorderapp.voice_recorder.domain.player

enum class PlayerState {

	IDLE,
	PLAYER_READY,
	COMPLETED;

	val canAdvertiseCurrentPosition: Boolean
		get() = this == PlayerState.PLAYER_READY

}