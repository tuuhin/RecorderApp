package com.eva.player_shared

import androidx.lifecycle.viewModelScope
import com.eva.interactions.domain.AppShortcutFacade
import com.eva.interactions.domain.ShareRecordingsUtil
import com.eva.player_shared.factory.PlayerMetadataViewmodelFactory
import com.eva.player_shared.state.ContentLoadState
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.recordings.domain.provider.RecordingsSecondaryDataProvider
import com.eva.ui.viewmodel.AppViewModel
import com.eva.ui.viewmodel.UIEvents
import com.eva.use_case.usecases.PlayerFileProviderFromIdUseCase
import com.eva.utils.Resource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = PlayerMetadataViewmodelFactory::class)
class PlayerMetadataViewmodel @AssistedInject constructor(
    @Assisted private val audioId: Long,
    private val fileProviderUseCase: PlayerFileProviderFromIdUseCase,
    private val metadataProvider: RecordingsSecondaryDataProvider,
    private val shareRecording: ShareRecordingsUtil,
    private val shortcutFacade: AppShortcutFacade,
) : AppViewModel() {

    private val _currentAudio = MutableStateFlow<AudioFileModel?>(null)
    private val _isAudioLoaded = MutableStateFlow(false)


    val loadState = combine(_isAudioLoaded, _currentAudio, transform = ::prepareLoadState)
        .onStart {
            loadAudioFile()
            setShortCut()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(8_000),
            initialValue = ContentLoadState.Loading,
        )


    override val uiEvent: SharedFlow<UIEvents>
        field = MutableSharedFlow<UIEvents>()

    private fun prepareLoadState(isLoaded: Boolean, audio: AudioFileModel?)
        : ContentLoadState<AudioFileModel> {
        return when {
            !isLoaded -> ContentLoadState.Loading
            audio != null -> ContentLoadState.Content(audio)
            else -> ContentLoadState.Unknown
        }
    }

    private fun setShortCut() {
        // set shortcut only when resource is loaded distinct by id ensures not to call it again
        // for simple metadata change
        _currentAudio.filterNotNull().distinctUntilChangedBy { it.id }
            .onEach { model -> shortcutFacade.addLastPlayedShortcut(model.id) }
            .launchIn(viewModelScope)
    }

    private fun loadAudioFile() = fileProviderUseCase.invoke(audioId)
        .onEach { res ->
            when (res) {
                Resource.Loading -> _isAudioLoaded.update { false }
                is Resource.Error -> {
                    _isAudioLoaded.update { true }
                    val message = res.message ?: res.error.message ?: ""
                    uiEvent.emit(UIEvents.ShowSnackBar(message))
                }

                is Resource.Success -> {
                    _isAudioLoaded.update { true }
                    _currentAudio.update { res.data }
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
            uiEvent.emit(UIEvents.ShowToast("Cannot share audio file"))
        }
    }

    private fun toggleIsFavourite(fileModel: AudioFileModel) {
        val contentState = (loadState.value as? ContentLoadState.Content) ?: return
        val isAlreadyFav = contentState.data.isFavourite

        viewModelScope.launch {
            when (val result = metadataProvider.favouriteAudioFile(fileModel, !isAlreadyFav)) {
                is Resource.Error -> {
                    val message = result.message ?: result.error.message ?: "fav cannot"
                    uiEvent.emit(UIEvents.ShowSnackBar(message))
                }

                is Resource.Success -> {
                    val message = result.message ?: "fav marked"
                    uiEvent.emit(UIEvents.ShowSnackBar(message))
                }

                else -> {}
            }
        }
    }
}
