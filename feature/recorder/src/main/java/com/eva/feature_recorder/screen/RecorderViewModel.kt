package com.eva.feature_recorder.screen

import androidx.lifecycle.viewModelScope
import com.eva.recorder.domain.RecorderActionHandler
import com.eva.recorder.domain.RecorderServiceBinder
import com.eva.recorder.domain.models.RecorderAction
import com.eva.recorder.domain.models.RecorderState
import com.eva.ui.viewmodel.AppViewModel
import com.eva.ui.viewmodel.UIEvents
import com.eva.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import javax.inject.Inject

@HiltViewModel
internal class RecorderViewModel @Inject constructor(
	private val handler: RecorderActionHandler,
	private val recorderService: RecorderServiceBinder,
) : AppViewModel() {

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()

	val isServiceReady: StateFlow<Boolean> = recorderService.isConnectionReady

	val recorderState = recorderService.recorderState
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(10_000),
			initialValue = RecorderState.IDLE
		)

	val recorderTime = recorderService.recorderTimer
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(10_000),
			initialValue = LocalTime(0, 0)
		)

	val bookMarksSet = recorderService.bookMarkTimes
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(10_000),
			initialValue = emptySet()
		)

	val recordingPoints = recorderService.amplitudes
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(10_000),
			initialValue = emptyList()
		)

	fun onAction(action: RecorderAction) {

		when (val resource = handler.onRecorderAction(action)) {
			is Resource.Error -> viewModelScope.launch {
				val message = resource.error.message ?: resource.message ?: ""
				_uiEvents.emit(UIEvents.ShowToast(message))
			}

			else -> {}
		}
	}


	fun onEvent(event: RecorderScreenEvent) {
		when (event) {
			RecorderScreenEvent.BindRecorderService -> recorderService.bindToService()
			RecorderScreenEvent.UnBindRecorderService -> recorderService.unBindService()
		}
	}

	override fun onCleared() {
		recorderService.unBindService()
		recorderService.cleanUp()
	}
}