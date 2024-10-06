package com.eva.recorderapp.voice_recorder.presentation.recordings.rename_dialog

import android.app.RecoverableSecurityException
import android.os.Build
import androidx.activity.result.IntentSenderRequest
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.eva.recorderapp.common.AppViewModel
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.common.UIEvents
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.VoiceRecordingsProvider
import com.eva.recorderapp.voice_recorder.domain.use_cases.RenameRecordingUseCase
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavDialogs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RenameDialogViewModel @Inject constructor(
	private val renameUseCase: RenameRecordingUseCase,
	private val recordingsProvider: VoiceRecordingsProvider,
	private val savedStateHandle: SavedStateHandle,
) : AppViewModel() {

	private val route: NavDialogs.RenameRecordingDialog
		get() = savedStateHandle.toRoute()

	private val recordingId: Long
		get() = route.recordingId

	private val _state = MutableStateFlow(RenameRecordingState())
	val renameState = _state.asStateFlow()

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()

	private val _permissionEvent = MutableSharedFlow<RenamePermissionEvent>()
	val permissionEvent = _permissionEvent.asSharedFlow()

	fun onEvent(event: RenameRecordingEvent) {
		when (event) {
			is RenameRecordingEvent.OnTextValueChange -> _state.update { state ->
				state.copy(textFieldState = event.textValue)
			}

			RenameRecordingEvent.OnRenameRecording -> renameRecording()
			is RenameRecordingEvent.OnWriteAccessChanged ->
				handleWriteActionResult(event.isAllowed, event.message)
		}
	}

	init {
		viewModelScope.launch { loadEntry() }
	}

	private suspend fun loadEntry() {
		when (val result = recordingsProvider.getVoiceRecordingAsResourceFromId(recordingId)) {
			is Resource.Error -> {
				val message = result.error.message ?: result.message ?: "Recording not found"
				_state.update { state ->
					state.copy(errorString = message, isRenameAllowed = false)
				}
				_uiEvents.emit(UIEvents.ShowToast(message))
			}

			is Resource.Success -> _state.update { state ->
				state.copy(
					recording = result.data,
					textFieldState = TextFieldValue(text = result.data.title),
					isRenameAllowed = true
				)
			}

			else -> {}
		}
	}

	private fun renameRecording() {
		val newName = _state.value.textFieldState.text.trim()

		if (newName.isEmpty() || newName.isBlank()) {
			_state.update { state -> state.copy(errorString = "Cannot have blank values") }
			return
		}

		renameUseCase(recordingId = recordingId, newName = newName)
			.onEach { result ->
				when (result) {
					Resource.Loading -> _state.update { it.copy(isRenameAllowed = true) }
					is Resource.Error -> {
						if (result.error is SecurityException) {
							// show the toast
							val message = result.message ?: "Access not allowed"
							_uiEvents.emit(UIEvents.ShowToast(message))

							// then handle security exception
							val recordingState = _state.updateAndGet { state ->
								state.copy(isRenameAllowed = false)
							}
							handleSecurityException(result.error, recordingState.recording)
							return@onEach
						}
						val message = result.message ?: result.error.message ?: ""
						_state.update { it.copy(errorString = message) }
					}

					is Resource.Success -> {
						val message = result.message ?: "Renamed recording successfully"
						_uiEvents.emit(UIEvents.ShowToast(message))
						_uiEvents.emit(UIEvents.PopScreen)
					}
				}
			}
			.launchIn(viewModelScope)
	}

	private fun handleWriteActionResult(isAccepted: Boolean, message: String) {
		_state.update { state -> state.copy(isRenameAllowed = isAccepted) }
		viewModelScope.launch { _uiEvents.emit(UIEvents.ShowToast(message)) }
	}

	private fun handleSecurityException(
		error: SecurityException,
		recording: RecordedVoiceModel? = null,
	) {
		if (recording == null) return

		val request = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			// event with recording
			RenamePermissionEvent.OnAskAccessRequest(recording)
		} else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && error is RecoverableSecurityException) {
			val pendingIntent = error.userAction.actionIntent
			val request = IntentSenderRequest.Builder(pendingIntent).build()
			// event with the intent sender
			RenamePermissionEvent.OnAskAccessRequest(
				recordings = recording,
				intentSenderRequest = request
			)

		} else null

		request?.let { viewModelScope.launch { _permissionEvent.emit(it) } }
	}
}