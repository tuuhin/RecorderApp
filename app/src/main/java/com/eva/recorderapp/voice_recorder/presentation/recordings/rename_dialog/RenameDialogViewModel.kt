package com.eva.recorderapp.voice_recorder.presentation.recordings.rename_dialog

import androidx.lifecycle.viewModelScope
import com.eva.recorderapp.common.DialogUIEvent
import com.eva.recorderapp.common.DialogViewModel
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.use_cases.RenameRecordingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class RenameDialogViewModel @Inject constructor(
	private val renameUseCase: RenameRecordingUseCase,
) : DialogViewModel() {

	private val _state = MutableStateFlow(RenameRecordingState())
	val renameState = _state.asStateFlow()

	private val _uiEvents = MutableSharedFlow<DialogUIEvent>()
	override val uiEvent: SharedFlow<DialogUIEvent>
		get() = _uiEvents.asSharedFlow()

	fun onEvent(event: RenameRecordingEvent) {
		when (event) {
			is RenameRecordingEvent.OnTextValueChange ->
				_state.update { state -> state.copy(textFieldState = event.textValue) }

			is RenameRecordingEvent.OnRenameRecording -> renameRecording(event.recordingId)
		}
	}

	private fun renameRecording(recordingId: Long) {
		val newName = _state.value.textFieldState.text.trim()

		if (newName.isEmpty() || newName.isBlank()) {
			_state.update { state -> state.copy(errorString = "Cannot have blank values") }
			return
		}

		renameUseCase(recordingId = recordingId, newName = newName)
			.onEach { result ->
				when (result) {
					Resource.Loading -> _state.update { it.copy(isRenaming = true) }
					is Resource.Error -> {
						val message = result.message ?: result.error.message ?: ""
						_state.update { it.copy(errorString = message) }
					}

					is Resource.Success -> {
						val message = result.message ?: "Renamed recording successfully"
						_uiEvents.emit(DialogUIEvent.ShowToast(message))
						_uiEvents.emit(DialogUIEvent.CloseDialog)
					}
				}
			}
			.launchIn(viewModelScope)
	}
}