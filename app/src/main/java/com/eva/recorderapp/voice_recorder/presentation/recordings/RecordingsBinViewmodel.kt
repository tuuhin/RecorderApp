package com.eva.recorderapp.voice_recorder.presentation.recordings

import android.app.RecoverableSecurityException
import android.os.Build
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.viewModelScope
import com.eva.recorderapp.common.AppViewModel
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.common.UIEvents
import com.eva.recorderapp.voice_recorder.domain.recordings.models.TrashRecordingModel
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.TrashRecordingsProvider
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.event.DeleteOrTrashRecordingsRequest
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.event.TrashRecordingScreenEvent
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.SelectableTrashRecordings
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.toSelectableRecordings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordingsBinViewmodel @Inject constructor(
	private val provider: TrashRecordingsProvider,
) : AppViewModel() {

	private val _trashedRecordings = MutableStateFlow(emptyList<SelectableTrashRecordings>())
	val trashRecordings = _trashedRecordings
		.map { it.toImmutableList() }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5000),
			initialValue = persistentListOf()
		)

	private val selectedRecordings: Collection<TrashRecordingModel>
		get() = _trashedRecordings.value
			.filter(SelectableTrashRecordings::isSelected)
			.map(SelectableTrashRecordings::trashRecording)


	private val _isRecordingsLoaded = MutableStateFlow(false)
	val isLoaded = _isRecordingsLoaded.asStateFlow()

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()

	private val _deleteEvents = MutableSharedFlow<DeleteOrTrashRecordingsRequest>()
	val deleteRequestEvent: SharedFlow<DeleteOrTrashRecordingsRequest>
		get() = _deleteEvents.asSharedFlow()


	private fun populateRecordings() {
		// recordings are already loaded no need to again add a collector
		if (_isRecordingsLoaded.value) return

		// recordings flow collector
		provider.trashedRecordingsFlow
			.onEach { res ->
				when (res) {
					is Resource.Error -> {
						val message = res.message ?: res.error.message ?: "SOME ERROR"
						_uiEvents.emit(UIEvents.ShowSnackBar(message = message))
						_isRecordingsLoaded.update { true }
					}

					Resource.Loading -> _isRecordingsLoaded.update { false }
					is Resource.Success -> {
						val new = res.data.toSelectableRecordings()

						_trashedRecordings.update { new }
						_isRecordingsLoaded.update { true }
					}
				}
			}.launchIn(viewModelScope)
	}

	fun onScreenEvent(event: TrashRecordingScreenEvent) {
		when (event) {
			TrashRecordingScreenEvent.PopulateTrashRecordings -> populateRecordings()
			TrashRecordingScreenEvent.OnSelectTrashRecording -> onSelectOrUnSelectAll(true)
			TrashRecordingScreenEvent.OnUnSelectTrashRecording -> onSelectOrUnSelectAll(false)
			TrashRecordingScreenEvent.OnSelectItemDeleteForeEver -> onPermanentDelete()
			TrashRecordingScreenEvent.OnSelectItemRestore -> onRecordingsRestore()
			is TrashRecordingScreenEvent.OnRecordingSelectOrUnSelect -> onToggleSelection(event.recording)
			is TrashRecordingScreenEvent.OnPostDeleteRequest -> viewModelScope.launch {
				_uiEvents.emit(UIEvents.ShowToast(event.message))
			}
		}
	}

	private fun onSelectOrUnSelectAll(select: Boolean = false) {
		_trashedRecordings.update { recordings ->
			recordings.map { record -> record.copy(isSelected = select) }
		}
	}

	private fun onRecordingsRestore() {
		viewModelScope.launch {
			when (val result = provider.restoreRecordingsFromTrash(selectedRecordings)) {
				is Resource.Error -> {
					val message = result.message ?: "Cannot restore items"
					_uiEvents.emit(UIEvents.ShowSnackBar(message))
				}

				is Resource.Success -> {
					val message = result.message ?: "Items restored"
					_uiEvents.emit(UIEvents.ShowToast(message))
				}

				else -> {}
			}
		}
	}

	private fun onPermanentDelete() = viewModelScope.launch {
		when (val result = provider.permanentlyDeleteRecordingsInTrash(selectedRecordings)) {
			is Resource.Error -> {
				if (result.error is SecurityException) {
					handleSecurityExceptionToDelete(result.error)
					return@launch
				}
				val message = result.error.message ?: result.message ?: "Cannot delete items"
				_uiEvents.emit(UIEvents.ShowSnackBar(message))
			}

			is Resource.Success -> {
				val message = result.message ?: "Items deleted successfully"
				_uiEvents.emit(UIEvents.ShowToast(message))
			}

			else -> {}
		}
	}

	private fun handleSecurityExceptionToDelete(error: SecurityException) {
		viewModelScope.launch {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

				val request = DeleteOrTrashRecordingsRequest.OnDeleteRequest(selectedRecordings)
				_deleteEvents.emit(request)

			} else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
				// TODO: Check the workflow for android 10
				(error as? RecoverableSecurityException)?.let { exp ->
					val pendingIntent = exp.userAction.actionIntent
					val request = IntentSenderRequest.Builder(pendingIntent)
						.build()
					val deleteRequest =
						DeleteOrTrashRecordingsRequest.OnDeleteRequest(
							trashRecordings = selectedRecordings,
							intentSenderRequest = request
						)
					_deleteEvents.emit(deleteRequest)
				}
			}
		}
	}


	private fun onToggleSelection(recording: TrashRecordingModel) {
		_trashedRecordings.update { recordings ->
			recordings.map { record ->
				if (record.trashRecording == recording)
					record.copy(isSelected = !record.isSelected)
				else record
			}
		}
	}
}