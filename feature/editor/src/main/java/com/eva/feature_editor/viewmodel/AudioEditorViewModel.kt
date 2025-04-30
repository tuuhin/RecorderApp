package com.eva.feature_editor.viewmodel

import androidx.lifecycle.viewModelScope
import com.eva.editor.data.AudioClipConfig
import com.eva.editor.domain.AudioTrimmer
import com.eva.editor.domain.SimpleAudioPlayer
import com.eva.editor.domain.TransformationProgress
import com.eva.feature_editor.event.EditorScreenEvent
import com.eva.player.domain.model.PlayerTrackData
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.ui.viewmodel.AppViewModel
import com.eva.ui.viewmodel.UIEvents
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = EditorViewmodelFactory::class)
internal class AudioEditorViewModel @AssistedInject constructor(
	@Assisted private val fileModel: AudioFileModel,
	private val trimmer: AudioTrimmer,
	private val player: SimpleAudioPlayer,
) : AppViewModel() {

	private val _clipConfig = MutableStateFlow(AudioClipConfig(end = fileModel.duration))
	val clipConfig = _clipConfig.asStateFlow()

	val isPlayerPlaying = player.isPlaying
		.onStart { setPlayerItem() }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.Eagerly,
			initialValue = false
		)

	val trackData = player.trackInfoAsFlow
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(1000L),
			initialValue = PlayerTrackData(total = fileModel.duration)
		)

	val transformInfo = trimmer.progress.stateIn(
		scope = viewModelScope,
		started = SharingStarted.Eagerly,
		initialValue = TransformationProgress.Idle
	)

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.onStart { readTransformationError() }
			.shareIn(viewModelScope, SharingStarted.Eagerly)


	fun onEvent(event: EditorScreenEvent) {
		when (event) {
			EditorScreenEvent.PauseAudio -> viewModelScope.launch { player.pausePlayer() }
			EditorScreenEvent.PlayAudio -> viewModelScope.launch { player.startOrResumePlayer() }
			is EditorScreenEvent.OnClipConfigChange -> {
				_clipConfig.update { event.config }
			}

			EditorScreenEvent.TrimSelectedArea -> {

			}

			is EditorScreenEvent.OnSeekTrack -> player.onSeekDuration(event.duration)
		}
	}


	fun setPlayerItem() = viewModelScope.launch {
		player.preparePlayer(fileModel)
	}


	private fun handleTrimmingAudio() = viewModelScope.launch {
		val clipData = _clipConfig.value
		if (!clipData.validate()) return@launch

		val result = trimmer.trimAudioFile(fileModel, _clipConfig.value)
		result.onFailure {
			_uiEvents.emit(UIEvents.ShowSnackBar(it.message ?: ""))
		}
	}

	private fun readTransformationError() = trimmer.errorsFlow
		.mapNotNull { it.message }
		.map { UIEvents.ShowSnackBar(it) }
		.onEach { event -> _uiEvents.emit(event) }
		.launchIn(viewModelScope)


	override fun onCleared() {
		player.cleanUp()
		super.onCleared()
	}
}