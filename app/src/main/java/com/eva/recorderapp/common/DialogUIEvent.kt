package com.eva.recorderapp.common

sealed interface DialogUIEvent {

	data class ShowToast(val message: String) : DialogUIEvent

	data object CloseDialog : DialogUIEvent
}