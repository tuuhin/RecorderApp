package com.eva.player.domain.model

enum class PlayerState {

	IDLE,
	PLAYER_READY,
	COMPLETED;

	val canAdvertiseCurrentPosition: Boolean
		get() = this == PLAYER_READY

}