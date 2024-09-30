package com.eva.recorderapp.voice_recorder.presentation.record_player

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.eva.recorderapp.common.AppViewModel
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.common.UIEvents
import com.eva.recorderapp.voice_recorder.data.player.MediaControllerProvider
import com.eva.recorderapp.voice_recorder.domain.bookmarks.RecordingBookmarksProvider
import com.eva.recorderapp.voice_recorder.domain.player.AudioFilePlayer
import com.eva.recorderapp.voice_recorder.domain.player.WaveformsReader
import com.eva.recorderapp.voice_recorder.domain.player.model.AudioFileModel
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.RecordingsSecondaryDataProvider
import com.eva.recorderapp.voice_recorder.domain.use_cases.PlayerFileProviderFromIdUseCase
import com.eva.recorderapp.voice_recorder.domain.util.AppShortcutFacade
import com.eva.recorderapp.voice_recorder.domain.util.ShareRecordingsUtil
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavRoutes
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.AudioFileEvent
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.AudioPlayerInformation
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.ContentLoadState
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.ControllerEvents
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.PlayerEvents
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.PlayerSliderControl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "PLAYER_VIEWMODEL"

@HiltViewModel
class AudioPlayerViewModel @Inject constructor(
	private val controller: MediaControllerProvider,
	private val fileProviderUseCase: PlayerFileProviderFromIdUseCase,
	private val secondaryRecordingsData: RecordingsSecondaryDataProvider,
	private val actionHelper: ShareRecordingsUtil,
	private val waveformsReader: WaveformsReader,
	private val shortcutsUtil: AppShortcutFacade,
	bookmarksProvider: RecordingBookmarksProvider,
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

	private val bookMarksFlow = bookmarksProvider.getRecordingBookmarksFromId(audioId)
		.map { it.toImmutableList() }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.Eagerly,
			initialValue = persistentListOf()
		)

	val waveforms = waveformsReader.wavefront.stateIn(
		scope = viewModelScope,
		started = SharingStarted.WhileSubscribed(8000),
		initialValue = emptyList()
	)

	// media load state
	val loadState = combine(
		flow = _isAudioLoaded,
		flow2 = _currentAudio,
		flow3 = controller.isControllerConnected,
		transform = ::prepareLoadState
	).stateIn(
		scope = viewModelScope,
		started = SharingStarted.Eagerly,
		initialValue = ContentLoadState.Loading
	)

	// player information
	val currentAudioState = combine(
		playerSliderControls.trackData,
		controller.playerMetaDataFlow,
		bookMarksFlow,
	) { trackData, metadata, bookmarks ->
		AudioPlayerInformation(
			trackData = trackData,
			playerMetaData = metadata,
			bookmarks = bookmarks
		)
	}.stateIn(
		scope = viewModelScope,
		started = SharingStarted.Eagerly,
		initialValue = AudioPlayerInformation()
	)

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()


	init {
		prepareCurrentRecording()
		computeWaveforms(audioId)
		preparePlayer()
		setShortcutForLastPlayed()
	}

	fun onControllerEvents(event: ControllerEvents) {
		when (event) {
			is ControllerEvents.OnAddController -> controller.prepareController(event.audioId)
			ControllerEvents.OnRemoveController -> controller.removeController()
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
		viewModelScope.launch {
			val contentState = (loadState.value as? ContentLoadState.Content) ?: return@launch
			val isAlreadyFav = contentState.data.isFavourite
			when (val result =
				secondaryRecordingsData.favouriteAudioFile(fileModel, !isAlreadyFav)) {
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
				Resource.Loading -> _currentAudio.value ?: kotlin.run {
					_isAudioLoaded.update { false }
				}

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


	private fun prepareLoadState(
		isLoaded: Boolean,
		audio: AudioFileModel?,
		isControllerConnected: Boolean,
	): ContentLoadState = when {
		!isLoaded || !isControllerConnected -> ContentLoadState.Loading
		audio != null -> ContentLoadState.Content(audio)
		else -> ContentLoadState.Unknown
	}

	private fun preparePlayer() {
		// make sure it's the same file
		val currentAudio = _currentAudio.filterNotNull()
			.distinctUntilChangedBy { it.id }

		combine(controller.playerFlow, currentAudio) { player, file ->
			when (val result = player.preparePlayer(file)) {
				is Resource.Error -> {
					val message = result.message ?: result.error.message ?: ""
					_uiEvents.emit(UIEvents.ShowSnackBar(message))
				}

				else -> {}
			}

		}.launchIn(viewModelScope)
	}

	private fun computeWaveforms(audioId: Long) = viewModelScope.launch {

		Log.d(TAG, "STARTING TO COMPUTE SAMPLES")

		val result = waveformsReader.performWaveformsReading(audioId)
		// if there is an error show the error
		(result as? Resource.Error)?.let { err ->
			val message = err.message ?: err.error.message ?: ""
			_uiEvents.emit(UIEvents.ShowSnackBar(message))
		}
	}

	private fun setShortcutForLastPlayed() {
		// it's a main thread work
		viewModelScope.launch(Dispatchers.Main) {
			// add last played shortcut
			shortcutsUtil.addLastPlayedShortcut(audioId)
		}
	}

	override fun onCleared() {
		//clear resources associated with reader
		waveformsReader.clearResources()
		Log.d(TAG, "CLEARING THE VIEWMODEL FOR AUDIO $audioId")
		super.onCleared()
	}
}