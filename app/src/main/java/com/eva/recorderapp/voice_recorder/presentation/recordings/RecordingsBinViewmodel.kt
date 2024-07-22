package com.eva.recorderapp.voice_recorder.presentation.recordings

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.eva.recorderapp.common.AppViewModel
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.common.UIEvents
import com.eva.recorderapp.voice_recorder.domain.files.TrashRecordingsProvider
import com.eva.recorderapp.voice_recorder.domain.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.SelectableRecordings
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.TrashScreenEvent
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.toSelectableRecordings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class RecordingsBinViewmodel @Inject constructor(
	private val provider: TrashRecordingsProvider
) : AppViewModel() {

	private val _trashedRecordings = MutableStateFlow(emptyList<SelectableRecordings>())
	val trashRecordings = _trashedRecordings
		.map { it.toImmutableList() }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5000),
			initialValue = persistentListOf()
		)

	private val _isRecodingsLoaded = MutableStateFlow(false)
	val isRecordingLoaded = _isRecodingsLoaded.asSharedFlow()

	val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()


	init {
		populateRecordings()
	}

	private fun populateRecordings() = provider.trashedRecordingsFlow
		.onEach { res ->
			Log.d("TAG", "$res")
			when (res) {
				is Resource.Error -> {
					_uiEvents.emit(
						UIEvents.ShowSnackBar(
							message = res.message ?: res.error.message ?: "SOME ERROR"
						)
					)
					_isRecodingsLoaded.update { true }
				}

				Resource.Loading -> _isRecodingsLoaded.update { false }
				is Resource.Success -> {
					val new = res.data.toSelectableRecordings()
					_trashedRecordings.update { new }

					_isRecodingsLoaded.update { true }
				}
			}

		}.launchIn(viewModelScope)

	fun onScreenEvent(event: TrashScreenEvent) {
		when (event) {
			is TrashScreenEvent.OnRecordingSelectOrUnSelect -> ontoggleRecordingSelection(event.recording)
			TrashScreenEvent.OnSelectTrashRecording -> onSelectOrUnSelectAllRecordings(true)
			TrashScreenEvent.OnUnSelectTrashRecording -> onSelectOrUnSelectAllRecordings(false)
			TrashScreenEvent.OnSelectItemDeleteForeEver -> {}
			TrashScreenEvent.OnSelectItemRestore -> {}
		}
	}

	private fun onSelectOrUnSelectAllRecordings(select: Boolean = false) {
		_trashedRecordings.update { recordings ->
			recordings.map { record -> record.copy(isSelected = select) }
		}
	}

	private fun ontoggleRecordingSelection(recording: RecordedVoiceModel) {
		_trashedRecordings.update { recordings ->
			recordings.map { record ->
				if (record.recoding == recording)
					record.copy(isSelected = !record.isSelected)
				else record
			}
		}
	}

}