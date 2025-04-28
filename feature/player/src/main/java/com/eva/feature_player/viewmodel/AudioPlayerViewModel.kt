package com.eva.feature_player.viewmodel

import androidx.lifecycle.viewModelScope
import com.eva.feature_player.state.AudioPlayerState
import com.eva.feature_player.state.ControllerEvents
import com.eva.feature_player.state.PlayerEvents
import com.eva.player.data.MediaControllerProvider
import com.eva.player.domain.AudioFilePlayer
import com.eva.player.domain.WaveformsReader
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.ui.viewmodel.AppViewModel
import com.eva.ui.viewmodel.UIEvents
import com.eva.utils.Resource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = PlayerViewmodelFactory::class)
internal class AudioPlayerViewModel @AssistedInject constructor(
	@Assisted val fileModel: AudioFileModel,
	private val controller: MediaControllerProvider,
	private val waveformsReader: WaveformsReader,
) : AppViewModel() {

	// audio player instance for calling the underlying app's
	private val audioPlayer: AudioFilePlayer?
		get() = controller.player

	val waveforms = waveformsReader.wavefront
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(8_000),
			initialValue = emptyList()
		)

	// player information
	val currentAudioState = combine(
		controller.trackInfoAsFlow,
		controller.playerMetaDataFlow,
		controller.isControllerConnected,
		transform = ::AudioPlayerState
	).onStart {
		setControllerIfReady()
		computeWaveforms()
	}
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(8_000),
			initialValue = AudioPlayerState()
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
		controller.isControllerConnected.onEach { isConnected ->
			if (!isConnected) return@onEach

			val result = controller.preparePlayer(fileModel) ?: return@onEach
			when (result) {
				is Resource.Error -> {
					val message = result.message ?: result.error.message ?: ""
					_uiEvents.emit(UIEvents.ShowSnackBar(message))
				}

				else -> {}
			}

		}
			.launchIn(viewModelScope)
	}


	private fun computeWaveforms() = viewModelScope.launch {
		val result = waveformsReader.readWaveformsFromFile(fileModel)
		// if there is an error show the error
		if (result is Resource.Error) {
			val message = result.message ?: result.error.message ?: ""
			_uiEvents.emit(UIEvents.ShowSnackBar(message))
		}
	}

	override fun onCleared() {
		//clear resources associated with reader
		waveformsReader.clearResources()
		// cleanup for controller
		controller.releaseController()
		super.onCleared()
	}
}