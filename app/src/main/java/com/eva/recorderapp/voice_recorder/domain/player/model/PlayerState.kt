package com.eva.recorderapp.voice_recorder.domain.player.model

enum class PlayerState {

	IDLE,
	PLAYER_READY,
	COMPLETED;

	val canAdvertiseCurrentPosition: Boolean
		get() = this == PLAYER_READY

}