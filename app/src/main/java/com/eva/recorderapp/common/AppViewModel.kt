package com.eva.recorderapp.common

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.SharedFlow

abstract class AppViewModel : ViewModel() {

	abstract val uiEvent: SharedFlow<UIEvents>
}