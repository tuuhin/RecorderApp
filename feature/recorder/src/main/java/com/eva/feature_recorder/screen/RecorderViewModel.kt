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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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

	val transcriptions = combine(
		recorderService.recorderState,
		controlledTranscriptions(),
	) { state, trans -> if (state.canReadAmplitudes) trans else null }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.Eagerly,
			null,
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

	@OptIn(ExperimentalCoroutinesApi::class)
	private fun controlledTranscriptions(slowDownDelay: Duration = 1.seconds) =
		recorderService.transcriptions.transformLatest { wordsStream ->
			val result = wordsStream.split(" ")
				.let { words -> if (words.size > 10) words.takeLast(10) else words }
				.joinToString(" ")

			emit(result)
			delay(slowDownDelay)
			emit(null)
		}


	override fun onCleared() {
		recorderService.unBindService()
		recorderService.cleanUp()
	}
}