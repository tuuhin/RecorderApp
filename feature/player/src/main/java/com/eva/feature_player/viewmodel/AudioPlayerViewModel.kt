package com.eva.feature_player.viewmodel

import androidx.lifecycle.viewModelScope
import com.eva.feature_player.state.ControllerEvents
import com.eva.feature_player.state.PlayerEvents
import com.eva.player.data.MediaControllerProvider
import com.eva.player.domain.AudioFilePlayer
import com.eva.player.domain.model.PlayerMetaData
import com.eva.player.domain.model.PlayerTrackData
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.recordings.domain.provider.PlayerFileProvider
import com.eva.ui.viewmodel.AppViewModel
import com.eva.ui.viewmodel.UIEvents
import com.eva.utils.Resource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = PlayerViewmodelFactory::class)
internal class AudioPlayerViewModel @AssistedInject constructor(
	@Assisted private val audioId: Long,
	private val fileProvider: PlayerFileProvider,
	private val controller: MediaControllerProvider,
) : AppViewModel() {

	// audio player instance for calling the underlying app's
	private val audioPlayer: AudioFilePlayer?
		get() = controller.player


	private val _currentFile = MutableStateFlow<AudioFileModel?>(null)
	private val _currentFileDistinctById = _currentFile
		.filterNotNull()
		.distinctUntilChangedBy { it.id }

	val playerMetaData = controller.playerMetaDataFlow.stateIn(
		scope = viewModelScope,
		started = SharingStarted.Lazily,
		initialValue = PlayerMetaData()
	)

	val trackData = controller.trackInfoAsFlow.stateIn(
		scope = viewModelScope,
		started = SharingStarted.WhileSubscribed(5_000),
		initialValue = PlayerTrackData()
	)

	val isControllerReady = controller.isControllerConnected
		.onStart {
			setAudioModel()
			setControllerIfReady()
		}.stateIn(
			scope = viewModelScope,
			started = SharingStarted.Eagerly,
			initialValue = false
		)

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()


	fun onControllerEvents(event: ControllerEvents) {
		when (event) {
			is ControllerEvents.OnAddController -> viewModelScope.launch {
				controller.prepareController(event.audioId)
			}

			ControllerEvents.OnRemoveController -> controller.releaseController()
		}
	}

	fun onPlayerEvents(event: PlayerEvents) {
		when (event) {
			PlayerEvents.OnPausePlayer -> viewModelScope.launch { audioPlayer?.pausePlayer() }
			PlayerEvents.OnStartPlayer -> viewModelScope.launch { audioPlayer?.startOrResumePlayer() }
			is PlayerEvents.OnForwardByNDuration ->
				audioPlayer?.seekPlayerByNDuration(duration = event.duration)

			is PlayerEvents.OnRewindByNDuration ->
				audioPlayer?.seekPlayerByNDuration(duration = event.duration, rewind = true)

			is PlayerEvents.OnPlayerSpeedChange -> audioPlayer?.setPlayBackSpeed(event.speed)
			is PlayerEvents.OnRepeatModeChange -> audioPlayer?.setPlayLooping(event.canRepeat)
			PlayerEvents.OnMutePlayer -> audioPlayer?.onMuteDevice()
			is PlayerEvents.OnSeekPlayer -> audioPlayer?.onSeekDuration(event.amount)
		}
	}

	private fun setControllerIfReady() {
		combine(
			controller.isControllerConnected,
			_currentFileDistinctById
		) { connected, fileModel ->
			if (!connected) return@combine

			val result = controller.preparePlayer(fileModel) ?: return@combine
			when (result) {
				is Resource.Error -> {
					val message = result.message ?: result.error.message ?: ""
					_uiEvents.emit(UIEvents.ShowSnackBar(message))
				}

				else -> {}
			}
		}.launchIn(viewModelScope)
	}


	private fun setAudioModel() = viewModelScope.launch {
		val result = fileProvider.getAudioFileFromId(audioId)
		when (result) {
			is Resource.Success -> _currentFile.updateAndGet { result.data }
			is Resource.Error -> _uiEvents.emit(UIEvents.ShowSnackBar(result.message ?: ""))
			else -> {}
		}
	}

	override fun onCleared() {
		// cleanup for controller
		controller.releaseController()
		super.onCleared()
	}
}