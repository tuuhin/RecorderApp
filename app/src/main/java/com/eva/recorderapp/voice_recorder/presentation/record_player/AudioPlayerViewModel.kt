package com.eva.recorderapp.voice_recorder.presentation.record_player

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.eva.recorderapp.common.AppViewModel
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.common.UIEvents
import com.eva.recorderapp.voice_recorder.data.player.AudioAmplitudeReader
import com.eva.recorderapp.voice_recorder.data.player.MediaControllerProvider
import com.eva.recorderapp.voice_recorder.domain.player.AudioFilePlayer
import com.eva.recorderapp.voice_recorder.domain.player.PlayerFileProvider
import com.eva.recorderapp.voice_recorder.domain.player.model.AudioFileModel
import com.eva.recorderapp.voice_recorder.domain.util.RecordingsActionHelper
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.AudioPlayerInformation
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.ContentLoadState
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.ControllerEvents
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.PlayerEvents
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.PlayerGraphInfo
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.PlayerSliderControl
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "PLAYER_VIEWMODEL"

@HiltViewModel(
	assistedFactory = AudioPlayerViewModelFactory::class
)
class AudioPlayerViewModel @AssistedInject constructor(
	@Assisted private val audioId: Long,
	private val controller: MediaControllerProvider,
	private val fileProvider: PlayerFileProvider,
	private val actionHelper: RecordingsActionHelper,
	private val samplesReader: AudioAmplitudeReader,
) : AppViewModel() {

	// audio player instance for calling the underlying api's
	private val audioPlayer: AudioFilePlayer?
		get() = controller.player

	//slider info
	private val playerSliderControls = PlayerSliderControl(controller)

	private val _currentAudio = MutableStateFlow<AudioFileModel?>(null)
	private val _isAudioLoaded = MutableStateFlow(false)

	@OptIn(ExperimentalCoroutinesApi::class)
	private val samplesToImmutableList = samplesReader.samples
		.map { data ->
			val builder = persistentListOf<Float>().builder()
			data.forEach(builder::add)
			builder.build()
		}.distinctUntilChanged()

	// graph data
	private val graphData = combine(
		samplesToImmutableList,
		samplesReader.isLoadingCompleted
	) { waves, isLoaded ->
		PlayerGraphInfo(
			isLoaded = isLoaded,
			waves = waves
		)
	}

	// media load state
	val loadState = combine(
		flow = _isAudioLoaded,
		flow2 = _currentAudio,
		flow3 = controller.isControllerConnected,
		transform = ::prepareLoadState
	).stateIn(
		scope = viewModelScope,
		started = SharingStarted.WhileSubscribed(3000),
		initialValue = ContentLoadState.Loading
	)

	// player information
	val playerInfo = combine(
		playerSliderControls.trackData,
		controller.playerMetaDataFlow,
		graphData,
	) { trackData, metadata, graph ->
		AudioPlayerInformation(
			trackData = trackData,
			playerMetaData = metadata,
			waveforms = graph
		)
	}.stateIn(
		scope = viewModelScope,
		started = SharingStarted.WhileSubscribed(2000),
		initialValue = AudioPlayerInformation()
	)


	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()


	init {
		prepareCurrentRecording()
		preparePlayer()
		computeSamples()
	}

	fun onControllerEvents(event: ControllerEvents) {
		when (event) {
			ControllerEvents.OnAddController -> viewModelScope.launch {
				controller.prepareController(audioId)
			}

			ControllerEvents.OnRemoveController -> controller.removeController()
		}
	}

	fun onPlayerEvents(event: PlayerEvents) {
		when (event) {
			PlayerEvents.ShareCurrentAudioFile -> shareCurrentAudioFile()
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

	private fun shareCurrentAudioFile() = viewModelScope.launch {
		_currentAudio.value?.let(actionHelper::shareAudioFile) ?: run {
			_uiEvents.emit(UIEvents.ShowToast("Cannot share audio file"))
		}
	}

	private fun prepareCurrentRecording() {
		fileProvider
			.getAudioFileInfo(id = audioId)
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
					}
				}
			}.launchIn(viewModelScope)
	}

	private fun prepareLoadState(
		isLoaded: Boolean,
		audio: AudioFileModel?,
		isControllerConnected: Boolean
	): ContentLoadState = when {
		!isLoaded || !isControllerConnected -> ContentLoadState.Loading
		isLoaded && audio != null -> ContentLoadState.Content(audio)
		else -> ContentLoadState.Unknown
	}

	private fun preparePlayer() {

		val currentAudio = _currentAudio.filterNotNull()

		combine(controller.playerFlow, currentAudio) { player, file ->

			Log.d(TAG, "PREPARING PLAYER")
			val result = player.preparePlayer(file)

			when (result) {
				is Resource.Error -> _uiEvents.emit(UIEvents.ShowSnackBar(result.message ?: ""))
				else -> {}
			}

		}.launchIn(viewModelScope)
	}

	private fun computeSamples() = viewModelScope.launch {

		Log.d(TAG, "STARTING TO COMPUTE SAMPLES")

		val result = samplesReader.evaluteSamplesGraphFromAudioId(audioId)
		// if there is an error show the error
		(result as? Resource.Error)?.let {
			_uiEvents.emit(UIEvents.ShowSnackBar(it.message ?: ""))
		}
	}

	override fun onCleared() {
		//clear codec
		samplesReader.clearResources()
		Log.d(TAG, "CLEARING THE VIEWMODEL FOR AUDIO $audioId")
		super.onCleared()
	}
}
