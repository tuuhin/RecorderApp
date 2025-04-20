package com.eva.feature_player.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.eva.feature_player.state.AudioPlayerState
import com.eva.feature_player.state.ControllerEvents
import com.eva.feature_player.state.PlayerEvents
import com.eva.feature_player.util.PlayerSliderControl
import com.eva.player.data.MediaControllerProvider
import com.eva.player.domain.AudioFilePlayer
import com.eva.player.domain.WaveformsReader
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.recordings.domain.provider.PlayerFileProvider
import com.eva.ui.navigation.NavRoutes
import com.eva.ui.viewmodel.AppViewModel
import com.eva.ui.viewmodel.UIEvents
import com.eva.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class AudioPlayerViewModel @Inject constructor(
	private val controller: MediaControllerProvider,
	private val waveformsReader: WaveformsReader,
	private val fileProvider: PlayerFileProvider,
	private val savedStateHandle: SavedStateHandle,
) : AppViewModel() {

	val route: NavRoutes.AudioPlayer
		get() = savedStateHandle.toRoute<NavRoutes.AudioPlayer>()

	private val audioId: Long
		get() = route.audioId

	// audio player instance for calling the underlying app's
	private val audioPlayer: AudioFilePlayer?
		get() = controller.player

	//slider info
	private val playerSliderControls = PlayerSliderControl(controller)

	val waveforms = waveformsReader.wavefront
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(8_000),
			initialValue = emptyList()
		)

	// player information
	val currentAudioState = combine(
		playerSliderControls.trackData,
		controller.playerMetaDataFlow,
		controller.isControllerConnected,
		transform = ::AudioPlayerState
	).onStart { readAudioFileFromId() }
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
			ControllerEvents.OnAddController -> viewModelScope.launch {
				controller.prepareController(audioId)
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
			is PlayerEvents.OnSeekPlayer -> playerSliderControls.onSliderSlide(event.amount)
			PlayerEvents.OnSeekComplete -> playerSliderControls.onSliderSlideComplete()
		}
	}

	private fun readAudioFileFromId() {
		val audioFileFlow = fileProvider.getAudioFileFromIdFlow(audioId)
			.map { res ->
				when (res) {
					is Resource.Success<AudioFileModel, Exception> -> res.data
					else -> null
				}
			}.filterNotNull()
			.distinctUntilChangedBy { it.id }

		// prepare the waveforms
		audioFileFlow.onEach { computeWaveforms(it) }.launchIn(viewModelScope)

		// prepare the player
		combine(audioFileFlow, controller.isControllerConnected) { audioFile, isPlayerReady ->
			if (isPlayerReady) prepareController(audioFile)
		}.launchIn(viewModelScope)
	}


	private suspend fun prepareController(audio: AudioFileModel) {
		val isConnected = controller.isControllerConnected.value
		if (!isConnected) return

		// if the controller is set we can load the item to play
		val result = controller.preparePlayer(audio) ?: return
		when (result) {
			is Resource.Error -> {
				val message = result.message ?: result.error.message ?: ""
				_uiEvents.emit(UIEvents.ShowSnackBar(message))
			}

			else -> {}
		}
	}


	private suspend fun computeWaveforms(audio: AudioFileModel) {
		val result = waveformsReader.readWaveformsFromFile(audio)
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