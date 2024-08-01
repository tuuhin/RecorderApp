package com.eva.recorderapp.voice_recorder.presentation.record_player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.eva.recorderapp.common.AppViewModel
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.common.UIEvents
import com.eva.recorderapp.voice_recorder.domain.files.RecordingsActionHelper
import com.eva.recorderapp.voice_recorder.domain.models.AudioFileModel
import com.eva.recorderapp.voice_recorder.domain.player.PlayerFileProvider
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavRoutes
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.ContentLoadState
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.PlayerEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioPlayerViewModel @Inject constructor(
	private val playerFileProvider: PlayerFileProvider,
	private val actionHadler: RecordingsActionHelper,
	private val savedStateHandle: SavedStateHandle,
) : AppViewModel() {

	private val _currentRecording = MutableStateFlow<AudioFileModel?>(null)
	private val _isAudioLoaded = MutableStateFlow(false)

	val contentLoadState = combine(_currentRecording, _isAudioLoaded) { audio, isLoaded ->
		when {
			!isLoaded -> ContentLoadState.Loading
			isLoaded && audio != null -> ContentLoadState.Content(audio)
			else -> ContentLoadState.Unknown
		}
	}.stateIn(
		scope = viewModelScope,
		started = SharingStarted.WhileSubscribed(3000),
		initialValue = ContentLoadState.Loading
	)

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()

	init {
		listenToPlayerParamChanges()
	}


	fun onPlayerEvents(event: PlayerEvents) {
		when (event) {
			PlayerEvents.ShareCurrentAudioFile -> shareCurrentAudioFile()
		}
	}

	private fun listenToPlayerParamChanges() {
		savedStateHandle.getStateFlow<Long?>(
			key = NavRoutes.AUDIO_PLAYER_PARAM_NAME,
			initialValue = null
		)
			.filterNotNull()
			.onEach(::prepareRecording)
			.launchIn(viewModelScope)
	}

	private fun shareCurrentAudioFile() = viewModelScope.launch {
		_currentRecording.value?.let(actionHadler::shareAudioFile) ?: run {
			_uiEvents.emit(UIEvents.ShowToast("Cannot share audio file"))
		}
	}

	private fun prepareRecording(id: Long) = playerFileProvider.getAudioFileInfo(id)
		.onEach { res ->
			when (res) {
				is Resource.Error -> {
					_isAudioLoaded.update { true }
					_uiEvents.emit(
						UIEvents.ShowSnackBar(message = res.message ?: res.error.message ?: "")
					)
				}

				Resource.Loading -> _isAudioLoaded.update { false }
				is Resource.Success -> {
					_isAudioLoaded.update { true }
					_currentRecording.update { res.data }
				}
			}
		}.launchIn(viewModelScope)
}
