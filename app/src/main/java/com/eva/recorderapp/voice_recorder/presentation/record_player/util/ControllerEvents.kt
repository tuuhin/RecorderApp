package com.eva.recorderapp.voice_recorder.presentation.record_player.util

sealed interface ControllerEvents {

	data object OnAddController : ControllerEvents

	data object OnRemoveController : ControllerEvents
}