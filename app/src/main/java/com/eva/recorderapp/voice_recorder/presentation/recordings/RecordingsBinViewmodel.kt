package com.eva.recorderapp.voice_recorder.presentation.recordings

import androidx.lifecycle.viewModelScope
import com.eva.recorderapp.common.AppViewModel
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.common.UIEvents
import com.eva.recorderapp.voice_recorder.domain.recordings.models.TrashRecordingModel
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.TrashRecordingsProvider
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

	private val areRecodingsLoaded = MutableStateFlow(false)
	val isLoaded = areRecodingsLoaded.asStateFlow()

	val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()


	init {
		populateRecordings()
	}

	private fun populateRecordings() = provider.trashedRecordingsFlow
		.onEach { res ->
			when (res) {
				is Resource.Error -> {
					val message = res.message ?: res.error.message ?: "SOME ERROR"
					_uiEvents.emit(UIEvents.ShowSnackBar(message = message))
					areRecodingsLoaded.update { true }
				}

				Resource.Loading -> areRecodingsLoaded.update { false }
				is Resource.Success -> {
					val new = res.data.toSelectableRecordings()

					_trashedRecordings.update { new }
					areRecodingsLoaded.update { true }
				}
			}

		}.launchIn(viewModelScope)

	fun onScreenEvent(event: TrashRecordingScreenEvent) {
		when (event) {
			is TrashRecordingScreenEvent.OnRecordingSelectOrUnSelect -> ontoggleRecordingSelection(event.recording)
			TrashRecordingScreenEvent.OnSelectTrashRecording -> onSelectOrUnSelectAllRecordings(true)
			TrashRecordingScreenEvent.OnUnSelectTrashRecording -> onSelectOrUnSelectAllRecordings(false)
			TrashRecordingScreenEvent.OnSelectItemDeleteForeEver -> onPermanentDelete()
			TrashRecordingScreenEvent.OnSelectItemRestore -> onRecordingsRestore()
		}
	}

	private fun onSelectOrUnSelectAllRecordings(select: Boolean = false) {
		_trashedRecordings.update { recordings ->
			recordings.map { record -> record.copy(isSelected = select) }
		}
	}

	private fun onRecordingsRestore() {
		val selected = _trashedRecordings.value
			.filter(SelectableTrashRecordings::isSelected)
			.map(SelectableTrashRecordings::trashRecording)

		viewModelScope.launch {
			val result = provider.restoreRecordingsFromTrash(selected)
			when (result) {
				is Resource.Error -> _uiEvents.emit(
					UIEvents.ShowToast(message = result.message ?: "Cannot restore items")
				)

				is Resource.Success -> _uiEvents.emit(
					UIEvents.ShowToast(message = result.message ?: "Items restored")
				)

				else -> {}
			}
		}
	}

	private fun onPermanentDelete() {
		val selected = _trashedRecordings.value
			.filter(SelectableTrashRecordings::isSelected)
			.map(SelectableTrashRecordings::trashRecording)

		viewModelScope.launch {
			val result = provider.permanentlyDeleteRecordedVoicesInTrash(selected)
			when (result) {
				is Resource.Error -> _uiEvents.emit(
					UIEvents.ShowToast(message = result.message ?: "Cannot restore items")
				)

				is Resource.Success -> _uiEvents.emit(
					UIEvents.ShowToast(message = result.message ?: "Items restored")
				)

				else -> {}
			}
		}
	}

	private fun ontoggleRecordingSelection(recording: TrashRecordingModel) {
		_trashedRecordings.update { recordings ->
			recordings.map { record ->
				if (record.trashRecording == recording)
					record.copy(isSelected = !record.isSelected)
				else record
			}
		}
	}

}