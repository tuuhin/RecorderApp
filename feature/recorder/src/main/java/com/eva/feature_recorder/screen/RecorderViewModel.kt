package com.eva.feature_recorder.screen

import androidx.lifecycle.viewModelScope
import com.eva.recorder.domain.RecorderActionHandler
import com.eva.recorder.domain.models.RecorderAction
import com.eva.ui.viewmodel.AppViewModel
import com.eva.ui.viewmodel.UIEvents
import com.eva.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class RecorderViewModel @Inject constructor(
	private val handler: RecorderActionHandler,
) : AppViewModel() {

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()

	fun onAction(action: RecorderAction) {

		when (val resource = handler.onRecorderAction(action)) {
			is Resource.Error -> viewModelScope.launch {
				val message = resource.error.message ?: resource.message ?: ""
				_uiEvents.emit(UIEvents.ShowToast(message))
			}

			else -> {}
		}
	}

}