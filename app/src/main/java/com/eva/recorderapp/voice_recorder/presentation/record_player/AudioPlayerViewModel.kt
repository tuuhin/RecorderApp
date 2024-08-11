package com.eva.recorderapp.voice_recorder.presentation.record_player

import androidx.lifecycle.viewModelScope
import com.eva.recorderapp.common.AppViewModel
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.common.UIEvents
import com.eva.recorderapp.voice_recorder.data.player.AudioAmplitudeReader
import com.eva.recorderapp.voice_recorder.domain.files.RecordingsActionHelper
import com.eva.recorderapp.voice_recorder.domain.models.AudioFileModel
import com.eva.recorderapp.voice_recorder.domain.player.AudioFilePlayer
import com.eva.recorderapp.voice_recorder.domain.player.PlayerFileProvider
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.AudioPlayerInformation
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.ContentLoadState
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.PlayerEvents
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.PlayerGraphInfo
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.PlayerSliderControl
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
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

@HiltViewModel(
	assistedFactory = AudioPlayerViewModelFactory::class
)
class AudioPlayerViewModel @AssistedInject constructor(
	@Assisted private val audioPlayer: AudioFilePlayer,
	@Assisted private val audioId: Long,
	private val fileProvider: PlayerFileProvider,
	private val actionHelper: RecordingsActionHelper,
	private val samplesReader: AudioAmplitudeReader,
) : AppViewModel() {

	//slider info
	private val playerSliderControls = PlayerSliderControl(audioPlayer)

	private val _currentAudio = MutableStateFlow<AudioFileModel?>(null)
	private val _isAudioLoaded = MutableStateFlow(false)
	private val _isPlayerPrepared = MutableStateFlow(false)

	private val samplesIntoImmutableList = samplesReader.samples.map { data ->
		val builder = persistentListOf<Float>().builder()
		data.forEach(builder::add)
		builder.build()
	}.distinctUntilChanged()

	// graph data
	private val graphData = combine(
		samplesIntoImmutableList,
		samplesReader.isLoadingCompleted
	) { waves, isLoaded -> PlayerGraphInfo(isLoaded = isLoaded, waves = waves) }

	// media load state
	val loadState = combine(_isAudioLoaded, _currentAudio, transform = ::prepareLoadState)
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(3000),
			initialValue = ContentLoadState.Loading
		)

	// player information
	val playerInfo = combine(
		playerSliderControls.trackData,
		audioPlayer.playerMetaDataFlow,
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
		preparePlayer()
		prepareCurrentRecording()
		computeSamples()
	}

	fun onPlayerEvents(event: PlayerEvents) {
		when (event) {
			PlayerEvents.ShareCurrentAudioFile -> shareCurrentAudioFile()
			PlayerEvents.OnPausePlayer -> viewModelScope.launch { audioPlayer.pausePlayer() }
			PlayerEvents.OnStartPlayer -> viewModelScope.launch { audioPlayer.startOrResumePlayer() }
			is PlayerEvents.OnForwardByNDuration -> audioPlayer.forwardPlayerByNDuration(event.duration)
			is PlayerEvents.OnRewindByNDuration -> audioPlayer.rewindPlayerByNDuration(event.duration)
			is PlayerEvents.OnPlayerSpeedChange -> audioPlayer.setPlayBackSpeed(event.speed)
			is PlayerEvents.OnRepeatModeChange -> audioPlayer.setPlayLooping(event.canRepeat)
			PlayerEvents.OnMutePlayer -> audioPlayer.onMuteDevice()
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
		fileProvider.getAudioFileInfo(id = audioId)
			.onEach { res ->
				when (res) {
					is Resource.Error -> {
						_isAudioLoaded.update { true }
						val message = res.message ?: res.error.message ?: ""
						_uiEvents.emit(UIEvents.ShowSnackBar(message))
					}

					Resource.Loading -> _isAudioLoaded.update { false }
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
	): ContentLoadState {
		return when {
			!isLoaded -> ContentLoadState.Loading
			isLoaded && audio != null -> ContentLoadState.Content(audio)
			else -> ContentLoadState.Unknown
		}
	}

	private fun preparePlayer() {
		_currentAudio.filterNotNull()
			// ensures renaming it don't call the player again
			.distinctUntilChanged { old, new -> old.id == new.id }
			.onEach { model ->
				if (_isPlayerPrepared.value) return@onEach
				// set isconfigured to true
				audioPlayer.preparePlayer(model)
				_isPlayerPrepared.update { true }
			}
			.launchIn(viewModelScope)
	}

	private fun computeSamples() = viewModelScope.launch {
		val result = samplesReader.evaluteSamplesGraphFromAudioId(audioId)
		// if there is an error show the error
		(result as? Resource.Error)?.let {
			_uiEvents.emit(UIEvents.ShowSnackBar(it.message ?: ""))
		}
	}

	override fun onCleared() {
		//clear codec
		samplesReader.clearResources()
		// clearing the player resources
		audioPlayer.clearResources()
		super.onCleared()
	}
}
