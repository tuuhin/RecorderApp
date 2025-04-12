package com.eva.feature_player.state

sealed interface ControllerEvents {

	data object OnAddController : ControllerEvents

	data object OnRemoveController : ControllerEvents
}