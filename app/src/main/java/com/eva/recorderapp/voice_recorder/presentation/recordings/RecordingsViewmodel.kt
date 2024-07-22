package com.eva.recorderapp.voice_recorder.presentation.recordings

import androidx.lifecycle.viewModelScope
import com.eva.recorderapp.common.AppViewModel
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.common.UIEvents
import com.eva.recorderapp.voice_recorder.domain.files.VoiceRecordingsProvider
import com.eva.recorderapp.voice_recorder.domain.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.RecordingScreenEvent
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.SelectableRecordings
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordingsViewmodel @Inject constructor(
	private val recordingsProvider: VoiceRecordingsProvider,
) : AppViewModel() {

	private val _recordings = MutableStateFlow(emptyList<SelectableRecordings>())
	val recordings = _recordings
		.map { it.toImmutableList() }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5000),
			initialValue = persistentListOf()
		)

	private val _isRecodingsLoaded = MutableStateFlow(false)
	val isRecordingLoaded = _isRecodingsLoaded.asSharedFlow()

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()

	init {
		populateRecordings()
	}

	private fun populateRecordings() = recordingsProvider.voiceRecordingsFlow
		.onEach { res ->
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
					_recordings.update { new }

					_isRecodingsLoaded.update { true }
				}
			}

		}.launchIn(viewModelScope)


	fun onScreenEvent(event: RecordingScreenEvent) {
		when (event) {
			is RecordingScreenEvent.OnRecordingSelectOrUnSelect -> ontoggleRecordingSelection(event.recording)
			RecordingScreenEvent.OnSelectAllRecordings -> onSelectOrUnSelectAllRecordings(true)
			RecordingScreenEvent.OnUnSelectAllRecordings -> onSelectOrUnSelectAllRecordings(false)
			RecordingScreenEvent.OnSelectedItemTrashRequest -> {}
		}
	}

	private fun onSelectOrUnSelectAllRecordings(select: Boolean = false) {
		_recordings.update { recordings ->
			recordings.map { record -> record.copy(isSelected = select) }
		}
	}

	private fun onTrashSelectedRecordings() {
		val selected = _recordings.value.filter { it.isSelected }.map { it.recoding }
		viewModelScope.launch {
			val result = recordingsProvider.createTrashRecordings(selected)
			when (result) {
				is Resource.Error -> _uiEvents.emit(
					UIEvents.ShowSnackBar(result.message ?: "CANNOT DELETE")
				)

				is Resource.Success -> TODO()
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


}