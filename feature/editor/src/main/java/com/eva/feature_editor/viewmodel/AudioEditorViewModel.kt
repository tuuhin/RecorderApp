package com.eva.feature_editor.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.eva.editor.domain.AudioTrimmer
import com.eva.feature_editor.event.AudioClipConfig
import com.eva.feature_editor.event.EditorScreenEvent
import com.eva.player.data.reader.PlainAudioVisualizer
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
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.time.measureTime

@HiltViewModel(assistedFactory = EditorViewmodelFactory::class)
internal class AudioEditorViewModel @AssistedInject constructor(
	@Assisted private val fileModel: AudioFileModel,
	private val visualizer: PlainAudioVisualizer,
	private val trimmer: AudioTrimmer,
) : AppViewModel() {

	private val _clipConfig = MutableStateFlow(AudioClipConfig(end = fileModel.duration))
	val clipConfig = _clipConfig.asStateFlow()


	val visuals = visualizer.visualization
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.Lazily,
			initialValue = emptyList()
		)

	val isPlayerPlaying = emptyFlow<Boolean>()
		.onStart { loadVisualizer() }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.Eagerly,
			initialValue = false
		)

	val trackData = emptyFlow<PlayerTrackData>()
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(1000L),
			initialValue = PlayerTrackData(total = fileModel.duration)
		)

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents


	fun onEvent(event: EditorScreenEvent) {
		when (event) {
			EditorScreenEvent.PauseAudio -> {}
			EditorScreenEvent.PlayAudio -> {}
			is EditorScreenEvent.OnClipConfigChange -> _clipConfig.update { event.config }
		}
	}

	fun loadVisualizer() {
		val time = measureTime {
			visualizer.prepareVisualization(fileModel, 110)
		}
		Log.d("VIEWMODEL_VISUALIZER", "$time")
	}

	override fun onCleared() {
//		player.cleanUp()
		visualizer.cleanUp()
		super.onCleared()
	}
}