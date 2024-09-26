package com.eva.recorderapp.voice_recorder.presentation.recorder

import androidx.lifecycle.viewModelScope
import com.eva.recorderapp.common.AppViewModel
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.common.UIEvents
import com.eva.recorderapp.voice_recorder.domain.recorder.RecorderActionHandler
import com.eva.recorderapp.voice_recorder.domain.recorder.emums.RecorderAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecorderViewModel @Inject constructor(
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