package com.eva.recorderapp.common

interface UIEvents {

	data class ShowSnackBarWithActions(
		val message: String,
		val action: () -> Unit = {},
		val actionText: String? = null,
		val long: Boolean = false,
	) : UIEvents

	data class ShowSnackBar(val message: String) : UIEvents

	data class ShowToast(val message: String) : UIEvents
}