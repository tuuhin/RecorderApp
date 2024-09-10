package com.eva.recorderapp.common

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.SharedFlow

// TODO: Rename it to screen viewmodel
abstract class AppViewModel : ViewModel() {

	abstract val uiEvent: SharedFlow<UIEvents>
}

abstract class DialogViewModel : ViewModel() {

	abstract val uiEvent: SharedFlow<DialogUIEvent>
}