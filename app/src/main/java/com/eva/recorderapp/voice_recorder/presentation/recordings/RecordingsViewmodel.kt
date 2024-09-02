package com.eva.recorderapp.voice_recorder.presentation.recordings

import android.os.Build
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewModelScope
import com.eva.recorderapp.common.AppViewModel
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.common.UIEvents
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.TrashRecordingsProvider
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.VoiceRecordingsProvider
import com.eva.recorderapp.voice_recorder.domain.util.RecordingsActionHelper
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.event.DeleteOrTrashRecordingsRequest
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.event.RecordingScreenEvent
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.event.RenameRecordingEvents
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.RecordingsSortInfo
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.RenameRecordingState
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.SelectableRecordings
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.SortOptions
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.sortSelector
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordingsViewmodel @Inject constructor(
	private val recordingsProvider: VoiceRecordingsProvider,
	private val trashProvider: TrashRecordingsProvider,
	private val shareRecordingsHelper: RecordingsActionHelper,
) : AppViewModel() {

	private val _sortInfo = MutableStateFlow(RecordingsSortInfo())
	val sortInfo = _sortInfo.asStateFlow()

	private val _renameState = MutableStateFlow(RenameRecordingState())
	val renameState = _renameState.asStateFlow()

	private val _recordings = MutableStateFlow(emptyList<SelectableRecordings>())
	val recordings = combine(_recordings, _sortInfo, transform = ::sortedResults)
		.map { it.toImmutableList() }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5000),
			initialValue = persistentListOf()
		)

	private val selectedRecordings: List<RecordedVoiceModel>
		get() = _recordings.value.filter(SelectableRecordings::isSelected)
			.map(SelectableRecordings::recoding)

	private val _isRecordingsLoaded = MutableStateFlow(false)
	val isLoaded = _isRecordingsLoaded.asStateFlow()

	private val _deleteEvents = MutableSharedFlow<DeleteOrTrashRecordingsRequest>()
	val trashRequestEvent: SharedFlow<DeleteOrTrashRecordingsRequest>
		get() = _deleteEvents.asSharedFlow()

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()


	init {
		populateRecordings()
	}

	private fun populateRecordings() = recordingsProvider.voiceRecordingsFlow
		.onEach { res ->
			when (res) {
				Resource.Loading -> _isRecordingsLoaded.update { false }
				is Resource.Error -> {
					val message = res.message ?: res.error.message ?: "SOME ERROR"
					_uiEvents.emit(
						UIEvents.ShowSnackBarWithActions(
							message = message,
							actionText = "Retry",
							action = ::populateRecordings
						)
					)
					_isRecordingsLoaded.update { true }
				}

				is Resource.Success -> {
					val new = res.data.toSelectableRecordings()
					_recordings.update { new }
					_isRecordingsLoaded.update { true }
				}
			}
		}.launchIn(viewModelScope)


	fun onScreenEvent(event: RecordingScreenEvent) {
		when (event) {
			is RecordingScreenEvent.OnRecordingSelectOrUnSelect -> toggleRecordingSelection(event.recording)
			RecordingScreenEvent.OnSelectAllRecordings -> onSelectOrUnSelectAllRecordings(true)
			RecordingScreenEvent.OnUnSelectAllRecordings -> onSelectOrUnSelectAllRecordings(false)
			RecordingScreenEvent.OnSelectedItemTrashRequest -> onTrashSelectedRecordings()
			is RecordingScreenEvent.OnSortOptionChange -> _sortInfo.update { it.copy(options = event.sort) }
			is RecordingScreenEvent.OnSortOrderChange -> _sortInfo.update { it.copy(order = event.order) }
			RecordingScreenEvent.ShareSelectedRecordings -> shareSelectedRecordings()
		}
	}

	fun onRenameRecordingEvent(event: RenameRecordingEvents) {
		when (event) {
			RenameRecordingEvents.OnCancelRenameRecording -> _renameState.update { state ->
				state.copy(showDialog = false, textFieldState = TextFieldValue())
			}

			is RenameRecordingEvents.OnTextValueChange -> _renameState.update { state ->
				state.copy(textFieldState = event.textValue)
			}

			RenameRecordingEvents.OnShowRenameDialog -> viewModelScope.launch {
				if (selectedRecordings.size > 1) {
					_uiEvents.emit(UIEvents.ShowSnackBar("Cannot modify more than one recording"))
					return@launch
				} else _renameState.update { state -> state.copy(showDialog = true) }
			}

			RenameRecordingEvents.OnRenameRecording -> renameRecording()
		}
	}

	private fun onSelectOrUnSelectAllRecordings(select: Boolean = false) {
		_recordings.update { recordings ->
			recordings.map { record -> record.copy(isSelected = select) }
		}
	}


	private fun sortedResults(
		recordings: List<SelectableRecordings>,
		sortInfo: RecordingsSortInfo
	): List<SelectableRecordings> = when (sortInfo.options) {
		SortOptions.DATE_CREATED -> recordings.sortSelector(sortInfo.order) { it.recoding.recordedAt }
		SortOptions.DURATION -> recordings.sortSelector(sortInfo.order) { it.recoding.duration }
		SortOptions.NAME -> recordings.sortSelector(sortInfo.order) { it.recoding.displayName }
		SortOptions.SIZE -> recordings.sortSelector(sortInfo.order) { it.recoding.sizeInBytes }
	}


	private fun onTrashSelectedRecordings() = viewModelScope.launch {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			// delete via intent
			val req = DeleteOrTrashRecordingsRequest.OnTrashRequest(selectedRecordings)
			_deleteEvents.emit(req)
		} else {
			// delete via content providers
			when (val result = trashProvider.createTrashRecordings(selectedRecordings)) {
				is Resource.Error -> {
					val message = result.message ?: "Cannot move items to trash"
					_uiEvents.emit(UIEvents.ShowSnackBar(message))
				}

				is Resource.Success -> {
					val message = result.message ?: "Moved items to trash"
					_uiEvents.emit(UIEvents.ShowSnackBar(message))
				}

				else -> {}
			}
		}
	}


	private fun toggleRecordingSelection(recording: RecordedVoiceModel) {
		_recordings.update { recordings ->
			recordings.map { record ->
				if (record.recoding == recording)
					record.copy(isSelected = !record.isSelected)
				else record
			}
		}
	}

	private fun renameRecording() {
		val recording = selectedRecordings.firstOrNull() ?: return
		val newName = _renameState.value.textFieldState.text

		if (newName.isEmpty() || newName.isBlank()) {
			_renameState.update { state -> state.copy(isBlank = true) }
			return
		}

		recordingsProvider.renameRecording(recording = recording, newName = newName)
			.onEach { result ->
				when (result) {
					Resource.Loading -> _renameState.update { it.copy(isRenaming = true) }
					is Resource.Error -> {
						val event = UIEvents.ShowSnackBar(
							message = result.message ?: result.error.message ?: ""
						)
						_renameState.update { RenameRecordingState() }
						_uiEvents.emit(event)
					}

					is Resource.Success -> {
						val message = result.message ?: "Renamed recording successfully"
						_renameState.update { RenameRecordingState() }
						_uiEvents.emit(UIEvents.ShowToast(message))
					}
				}
			}.launchIn(viewModelScope)
	}


	private fun shareSelectedRecordings() {
		when (val res = shareRecordingsHelper.shareAudioFiles(selectedRecordings)) {
			is Resource.Error -> viewModelScope.launch {
				val message = res.message ?: "SOME ERROR SHARING RECORDING"
				_uiEvents.emit(UIEvents.ShowToast(message))
			}

			else -> {}
		}
	}
}