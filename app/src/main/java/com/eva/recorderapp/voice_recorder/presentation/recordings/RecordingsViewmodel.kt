package com.eva.recorderapp.voice_recorder.presentation.recordings

import android.util.Log
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewModelScope
import com.eva.recorderapp.common.AppViewModel
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.common.UIEvents
import com.eva.recorderapp.voice_recorder.domain.files.RecordingsActionHelper
import com.eva.recorderapp.voice_recorder.domain.files.TrashRecordingsProvider
import com.eva.recorderapp.voice_recorder.domain.files.VoiceRecordingsProvider
import com.eva.recorderapp.voice_recorder.domain.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.event.RecordingScreenEvent
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.event.RenameRecordingEvents
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.RecordingsSortInfo
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.RenameRecordingState
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.SelectableRecordings
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.SortOptions
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.SortOrder
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

	private val _isLoaded = MutableStateFlow(false)
	val isRecordingLoaded = _isLoaded.asStateFlow()

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()


	private val selectedRecordings: List<RecordedVoiceModel>
		get() = _recordings.value.filter(SelectableRecordings::isSelected)
			.map(SelectableRecordings::recoding)

	init {
		populateRecordings()
	}

	private fun populateRecordings() = recordingsProvider.voiceRecordingsFlow
		.onEach { res ->
			when (res) {
				is Resource.Error -> {
					_uiEvents.emit(
						UIEvents.ShowSnackBarWithActions(
							message = res.message ?: res.error.message ?: "SOME ERROR",
							actionText = "Retry",
							action = ::populateRecordings
						)
					)
					_isLoaded.update { true }
				}

				Resource.Loading -> _isLoaded.update { false }
				is Resource.Success -> {
					val new = res.data.toSelectableRecordings()
					_recordings.update { new }

					_isLoaded.update { true }
				}
			}

		}.launchIn(viewModelScope)


	fun onScreenEvent(event: RecordingScreenEvent) {
		when (event) {
			is RecordingScreenEvent.OnRecordingSelectOrUnSelect -> ontoggleRecordingSelection(event.recording)
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

			RenameRecordingEvents.OnRenameRecording -> renameRecording()
			is RenameRecordingEvents.OnTextValueChange -> _renameState.update { state ->
				state.copy(textFieldState = event.textValue)
			}

			RenameRecordingEvents.OnShowRenameDialog -> viewModelScope.launch {
				if (selectedRecordings.size > 1) {
					_uiEvents.emit(UIEvents.ShowSnackBar("Cannot modify more than one recording"))
					return@launch
				}
				_renameState.update { state -> state.copy(showDialog = true) }
			}

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
	): List<SelectableRecordings> {
		val sortOptionResult = when (sortInfo.options) {
			SortOptions.DATE_CREATED -> recordings.sortedBy { it.recoding.recordedAt }
			SortOptions.DURATION -> recordings.sortedBy { it.recoding.duration }
			SortOptions.NAME -> recordings.sortedBy { it.recoding.displayName }
		}
		return when (sortInfo.order) {
			SortOrder.ASC -> sortOptionResult
			SortOrder.DESC -> sortOptionResult.reversed()
		}
	}

	private fun onTrashSelectedRecordings() {

		viewModelScope.launch {
			val result = trashProvider.createTrashRecordings(selectedRecordings)
			when (result) {
				is Resource.Error -> _uiEvents.emit(
					UIEvents.ShowSnackBar(result.message ?: "Cannot move items to trash")
				)

				is Resource.Success -> _uiEvents.emit(
					UIEvents.ShowSnackBar(result.message ?: "Moved items to trash")
				)

				else -> {}
			}
		}
	}

	private fun ontoggleRecordingSelection(recording: RecordedVoiceModel) {
		_recordings.update { recordings ->
			recordings.map { record ->
				if (record.recoding == recording)
					record.copy(isSelected = !record.isSelected)
				else record
			}
		}
	}

	private fun renameRecording() = viewModelScope.launch {

		selectedRecordings.firstOrNull()?.let { recording ->
			Log.d("TAG", "RENAME SELECTION $selectedRecordings")

			val newName = _renameState.value.textFieldState.text

			if (newName.isEmpty() || newName.isBlank()) {
				_renameState.update { state -> state.copy(isBlank = true) }
				return@launch
			}

			recordingsProvider.renameRecording(recording, newName)
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
							_renameState.update { RenameRecordingState() }
							_uiEvents.emit(UIEvents.ShowToast("Updated recording name"))
						}
					}
				}.launchIn(this)

		} ?: run {
			_uiEvents.emit(UIEvents.ShowSnackBar("None are selected to renamed"))
		}

	}


	private fun shareSelectedRecordings() {
		when (val resource = shareRecordingsHelper.shareAudioFiles(selectedRecordings)) {
			is Resource.Error -> viewModelScope.launch {
				_uiEvents.emit(
					UIEvents.ShowToast(
						resource.message ?: "SOME ERROR SHARING RECORDING"
					)
				)
			}

			else -> {}
		}
	}

}