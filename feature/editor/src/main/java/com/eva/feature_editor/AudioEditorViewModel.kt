package com.eva.feature_editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.eva.editor.domain.AudioTrimmer
import com.eva.editor.domain.SimpleAudioPlayer
import com.eva.feature_editor.event.EditorScreenEvent
import com.eva.player.domain.model.PlayerTrackData
import com.eva.ui.navigation.NavRoutes
import com.eva.ui.viewmodel.AppViewModel
import com.eva.ui.viewmodel.UIEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
internal class AudioEditorViewModel @Inject constructor(
	private val player: SimpleAudioPlayer,
	private val trimmer: AudioTrimmer,
	private val savedStateHandle: SavedStateHandle,
) : AppViewModel() {

	val route: NavRoutes.AudioPlayer
		get() = savedStateHandle.toRoute<NavRoutes.AudioPlayer>()

	private val audioId: Long
		get() = route.audioId

	val isPlayerPlaying = player.isPlaying.stateIn(
		scope = viewModelScope,
		started = SharingStarted.Eagerly,
		initialValue = false
	)

	val trackData = player.trackInfoAsFlow.stateIn(
		scope = viewModelScope,
		started = SharingStarted.WhileSubscribed(1000L),
		initialValue = PlayerTrackData()
	)

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents


	fun onEvent(event: EditorScreenEvent) {
		when (event) {
			EditorScreenEvent.PauseAudio -> {}
			EditorScreenEvent.PlayAudio -> {}
			is EditorScreenEvent.OnClipConfigChange -> {}
		}
	}


	override fun onCleared() {
		player.cleanUp()
		super.onCleared()
	}
}