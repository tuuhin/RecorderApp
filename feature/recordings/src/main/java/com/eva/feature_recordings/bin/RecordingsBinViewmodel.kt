package com.eva.feature_recordings.bin

import android.app.RecoverableSecurityException
import android.os.Build
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.viewModelScope
import com.eva.feature_recordings.bin.state.SelectableTrashRecordings
import com.eva.feature_recordings.bin.state.TrashRecordingScreenEvent
import com.eva.feature_recordings.bin.state.toSelectableRecordings
import com.eva.feature_recordings.util.DeleteOrTrashRequestEvent
import com.eva.recordings.domain.models.TrashRecordingModel
import com.eva.recordings.domain.provider.TrashRecordingsProvider
import com.eva.ui.viewmodel.AppViewModel
import com.eva.ui.viewmodel.UIEvents
import com.eva.utils.Resource
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
internal class RecordingsBinViewmodel @Inject constructor(
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

	private val selectedRecordings: List<TrashRecordingModel>
		get() = _trashedRecordings.value
			.filter(SelectableTrashRecordings::isSelected)
			.map(SelectableTrashRecordings::trashRecording)


	private val _isRecordingsLoaded = MutableStateFlow(false)
	val isLoaded = _isRecordingsLoaded.asStateFlow()

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()

	private val _deleteEvents = MutableSharedFlow<DeleteOrTrashRequestEvent>()
	val deleteRequestEvent: SharedFlow<DeleteOrTrashRequestEvent>
		get() = _deleteEvents.asSharedFlow()


	private fun populateRecordings() {
		// recordings are already loaded no need to again add a collector
		if (_isRecordingsLoaded.value) return

		// recordings flow collector
		provider.trashedRecordingsFlow
			.onEach { res ->
				when (res) {
					Resource.Loading -> _isRecordingsLoaded.update { false }
					is Resource.Error -> {
						val message = res.message ?: res.error.message ?: "SOME ERROR"
						_uiEvents.emit(UIEvents.ShowSnackBar(message = message))
					}

					is Resource.Success -> {
						val new = res.data.toSelectableRecordings()
						_trashedRecordings.update { new }
					}
				}
				_isRecordingsLoaded.update { true }
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

	private fun onRecordingsRestore() = viewModelScope.launch {
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

	private fun onPermanentDelete() {
		provider.permanentlyDeleteRecordingsInTrash(selectedRecordings)
			.onEach { result ->
				when (result) {
					is Resource.Error -> {
						val error = result.error
						if (error is SecurityException) {
							val trashList = result.data ?: emptyList()
							// on security exception handle the case
							handleSecurityExceptionToDelete(error, trashList)
							return@onEach
						}
						val message =
							result.error.message ?: result.message ?: "Cannot delete items"
						_uiEvents.emit(UIEvents.ShowSnackBar(message))
					}

					is Resource.Success -> {
						val message = result.message ?: "Items deleted successfully"
						_uiEvents.emit(UIEvents.ShowToast(message))
					}

					else -> {}
				}
			}.launchIn(viewModelScope)

	}

	private fun handleSecurityExceptionToDelete(
		error: SecurityException,
		recordingsToDelete: Collection<TrashRecordingModel>,
		isEnabled: Boolean = false,
	) {
		if (recordingsToDelete.isEmpty() || !isEnabled) return
		if (error !is RecoverableSecurityException) return

		viewModelScope.launch {
			val request = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				DeleteOrTrashRequestEvent.OnDeleteRequest(recordingsToDelete)
			} else {
				// TODO: Check the workflow for android 10 or below
				val pendingIntent = error.userAction.actionIntent
				val request = IntentSenderRequest.Builder(pendingIntent).build()
				DeleteOrTrashRequestEvent.OnDeleteRequest(
					trashRecordings = selectedRecordings,
					intentSenderRequest = request
				)
			}
			_deleteEvents.emit(request)
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