package com.eva.feature_player.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.eva.feature_player.state.AudioFileEvent
import com.eva.feature_player.state.AudioPlayerState
import com.eva.feature_player.state.ContentLoadState
import com.eva.feature_player.state.ControllerEvents
import com.eva.feature_player.state.PlayerEvents
import com.eva.feature_player.util.PlayerSliderControl
import com.eva.interactions.domain.AppShortcutFacade
import com.eva.interactions.domain.ShareRecordingsUtil
import com.eva.player.data.MediaControllerProvider
import com.eva.player.domain.AudioFilePlayer
import com.eva.player.domain.WaveformsReader
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.recordings.domain.provider.RecordingsSecondaryDataProvider
import com.eva.ui.navigation.NavRoutes
import com.eva.ui.viewmodel.AppViewModel
import com.eva.ui.viewmodel.UIEvents
import com.eva.use_case.usecases.PlayerFileProviderFromIdUseCase
import com.eva.utils.Resource
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class AudioPlayerViewModel @Inject constructor(
	private val controller: MediaControllerProvider,
	private val fileProviderUseCase: PlayerFileProviderFromIdUseCase,
	private val metadataProvider: RecordingsSecondaryDataProvider,
	private val actionHelper: ShareRecordingsUtil,
	private val waveformsReader: WaveformsReader,
	private val shortcutsUtil: AppShortcutFacade,
	private val savedStateHandle: SavedStateHandle,
) : AppViewModel() {

	private val route: NavRoutes.AudioPlayer
		get() = savedStateHandle.toRoute<NavRoutes.AudioPlayer>()

	private val audioId: Long
		get() = route.audioId

	// audio player instance for calling the underlying app's
	private val audioPlayer: AudioFilePlayer?
		get() = controller.player

	//slider info
	private val playerSliderControls = PlayerSliderControl(controller)

	private val _currentAudio = MutableStateFlow<AudioFileModel?>(null)
	private val _isAudioLoaded = MutableStateFlow(false)

	val waveforms = waveformsReader.wavefront.stateIn(
		scope = viewModelScope,
		started = SharingStarted.Companion.WhileSubscribed(8000),
		initialValue = emptyList()
	)

	// media load state
	val loadState = combine(
		flow = _isAudioLoaded,
		flow2 = _currentAudio,
		transform = ::prepareLoadState
	).stateIn(
		scope = viewModelScope,
		started = SharingStarted.Companion.Eagerly,
		initialValue = ContentLoadState.Loading
	)

	// player information
	val currentAudioState = combine(
		playerSliderControls.trackData,
		controller.playerMetaDataFlow,
		controller.isControllerConnected,
		transform = ::AudioPlayerState
	).stateIn(
		scope = viewModelScope,
		started = SharingStarted.Companion.Eagerly,
		initialValue = AudioPlayerState()
	)

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()


	init {
		prepareCurrentRecording()
		computeWaveforms()
		preparePlayer()
		setShortcutForLastPlayed()
	}

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

	fun onFileEvents(event: AudioFileEvent) {
		when (event) {
			AudioFileEvent.ShareCurrentAudioFile -> shareCurrentAudioFile()
			is AudioFileEvent.ToggleIsFavourite -> toggleIsFavourite(event.file)
		}
	}


	private fun shareCurrentAudioFile() = viewModelScope.launch {
		_currentAudio.value?.let(actionHelper::shareAudioFile) ?: run {
			_uiEvents.emit(UIEvents.ShowToast("Cannot share audio file"))
		}
	}

	private fun toggleIsFavourite(fileModel: AudioFileModel) {
		val contentState = (loadState.value as? ContentLoadState.Content) ?: return
		val isAlreadyFav = contentState.data.isFavourite

		viewModelScope.launch {
			when (val result = metadataProvider.favouriteAudioFile(fileModel, !isAlreadyFav)) {
				is Resource.Error -> {
					val message = result.message ?: result.error.message ?: "fav cannot"
					_uiEvents.emit(UIEvents.ShowSnackBar(message))
				}

				is Resource.Success -> {
					val message = result.message ?: "fav marked"
					_uiEvents.emit(UIEvents.ShowSnackBar(message))
				}

				else -> {}
			}
		}
	}

	private fun prepareCurrentRecording() = fileProviderUseCase.invoke(audioId)
		.onEach { res ->
			when (res) {
				Resource.Loading -> _isAudioLoaded.update { false }
				is Resource.Error -> {
					_isAudioLoaded.update { true }
					val message = res.message ?: res.error.message ?: ""
					_uiEvents.emit(UIEvents.ShowSnackBar(message))
				}

				is Resource.Success -> {
					_isAudioLoaded.update { true }
					_currentAudio.update { res.data }
					// set shortcut only when resource is loaded
				}
			}
		}.launchIn(viewModelScope)


	private fun prepareLoadState(isLoaded: Boolean, audio: AudioFileModel?): ContentLoadState =
		when {
			!isLoaded -> ContentLoadState.Loading
			audio != null -> ContentLoadState.Content(audio)
			else -> ContentLoadState.Unknown
		}


	private fun preparePlayer() {
		val currentAudio = _currentAudio.filterNotNull()
			// ensures the file is not changed as file content can change
			.distinctUntilChangedBy { it.id }

		combine(controller.isControllerConnected, currentAudio) { isConnected, audio ->
			if (!isConnected) return@combine
			val result = controller.preparePlayer(audio) ?: return@combine
			when (result) {
				is Resource.Error -> {
					val message = result.message ?: result.error.message ?: ""
					_uiEvents.emit(UIEvents.ShowSnackBar(message))
				}

				else -> {}
			}

		}.launchIn(viewModelScope)
	}

	private fun computeWaveforms() {
		viewModelScope.launch {

			val result = waveformsReader.performWaveformsReading(audioId)
			// if there is an error show the error
			if (result is Resource.Error) {
				val message = result.message ?: result.error.message ?: ""
				_uiEvents.emit(UIEvents.ShowSnackBar(message))
			}
		}
	}

	private fun setShortcutForLastPlayed() {
		_currentAudio.filterNotNull()
			// ensures the file is not changed as file content can change
			.distinctUntilChangedBy { it.id }
			.onEach { model ->
				// this ensures shortcut is only added if the content is properly
				shortcutsUtil.addLastPlayedShortcut(model.id)
			}
			.launchIn(viewModelScope)
	}

	override fun onCleared() {
		//clear resources associated with reader
		waveformsReader.clearResources()
		// cleanup for controller
		controller.releaseController()
		super.onCleared()
	}
}