package com.eva.player_shared

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.eva.interactions.domain.AppShortcutFacade
import com.eva.interactions.domain.ShareRecordingsUtil
import com.eva.player_shared.state.ContentLoadState
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerMetadataViewmodel @Inject constructor(
	private val fileProviderUseCase: PlayerFileProviderFromIdUseCase,
	private val metadataProvider: RecordingsSecondaryDataProvider,
	private val shareRecording: ShareRecordingsUtil,
	private val shortcutFacade: AppShortcutFacade,
	private val savedStateHandle: SavedStateHandle,
) : AppViewModel() {

	private val route: NavRoutes.AudioPlayer
		get() = savedStateHandle.toRoute<NavRoutes.AudioPlayer>()

	val audioId: Long
		get() = route.audioId

	private val _currentAudio = MutableStateFlow<AudioFileModel?>(null)
	private val _isAudioLoaded = MutableStateFlow(false)

	val loadState = combine(_isAudioLoaded, _currentAudio, transform = ::prepareLoadState)
		.onStart { loadAudioFile() }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(8_000),
			initialValue = ContentLoadState.Loading
		)


	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents

	private fun prepareLoadState(isLoaded: Boolean, audio: AudioFileModel?)
			: ContentLoadState<AudioFileModel> {
		return when {
			!isLoaded -> ContentLoadState.Loading
			audio != null -> ContentLoadState.Content(audio)
			else -> ContentLoadState.Unknown
		}
	}

	private fun loadAudioFile() = fileProviderUseCase.invoke(audioId)
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
					val model = _currentAudio.updateAndGet { res.data } ?: res.data
					// set shortcut only when resource is loaded
					// this ensures shortcut is only added if the content is properly
					shortcutFacade.addLastPlayedShortcut(model.id)
				}
			}
		}.launchIn(viewModelScope)


	fun onFileEvent(event: UserAudioAction) {
		when (event) {
			UserAudioAction.ShareCurrentAudioFile -> shareCurrentAudioFile()
			is UserAudioAction.ToggleIsFavourite -> toggleIsFavourite(event.file)
		}
	}

	private fun shareCurrentAudioFile() = viewModelScope.launch {
		_currentAudio.value?.let(shareRecording::shareAudioFile) ?: run {
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
}