package com.eva.feature_player.state

sealed interface ControllerEvents {

	data class OnAddController(val audioId: Long) : ControllerEvents

	data object OnRemoveController : ControllerEvents
}