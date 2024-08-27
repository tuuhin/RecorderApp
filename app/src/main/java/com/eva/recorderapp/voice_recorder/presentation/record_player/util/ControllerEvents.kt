package com.eva.recorderapp.voice_recorder.presentation.record_player.util

sealed interface ControllerEvents {

	data class OnAddController(val audioId: Long) : ControllerEvents

	data object OnRemoveController : ControllerEvents
}