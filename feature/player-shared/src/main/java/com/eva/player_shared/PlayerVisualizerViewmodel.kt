package com.eva.player_shared

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.com.visualizer.data.compressFloatArray
import com.com.visualizer.domain.AudioVisualizer
import com.com.visualizer.domain.VisualizerState
import com.com.visualizer.domain.exception.DecoderExistsException
import com.eva.editor.domain.AudioConfigToActionList
import com.eva.player_shared.util.CoroutineLifecycleOwner
import com.eva.player_shared.util.updateArrayViaConfigs
import com.eva.recordings.domain.provider.PlayerFileProvider
import com.eva.ui.navigation.PlayerSubGraph
import com.eva.ui.viewmodel.AppViewModel
import com.eva.ui.viewmodel.UIEvents
import com.eva.utils.RecorderConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class PlayerVisualizerViewmodel @Inject constructor(
	private val visualizer: AudioVisualizer,
	private val playerFileProvider: PlayerFileProvider,
	private val savedStateHandle: SavedStateHandle,
) : AppViewModel() {

	private val _lifecycleOwner by lazy { CoroutineLifecycleOwner(viewModelScope.coroutineContext) }

	private val route: PlayerSubGraph.NavGraph
		get() = savedStateHandle.toRoute()

	private val _compressedVisualization = MutableStateFlow(floatArrayOf())
	private val _clipConfigs = MutableStateFlow<AudioConfigToActionList>(emptyList())

	// basic flag
	private var _isVisualizerStarted = false

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents

	val isVisualsReady = visualizer.visualizerState
		.map { it != VisualizerState.NOT_STARTED }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5_000L),
			initialValue = false
		)

	val fullVisualization = visualizer.normalizedVisualization
		.onStart { prepareVisuals() }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5_000),
			initialValue = floatArrayOf()
		)

	val compressedVisuals = _compressedVisualization
		.map { it.compressFloatArray(RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE) }
		.flowOn(Dispatchers.Default)
		.onStart { updatesOnConfigChange() }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5_000L),
			initialValue = floatArrayOf()
		)


	fun updateClipConfigs(configs: AudioConfigToActionList) = _clipConfigs.update { configs }


	private fun prepareVisuals() {
		// if started once don't start again
		if (!_isVisualizerStarted) return
		_isVisualizerStarted = true

		visualizer.visualizerState.onEach { state ->
			// only run this if the visualizer not in finished or running state
			if (state != VisualizerState.NOT_STARTED) return@onEach

			val result = visualizer.prepareVisualization(
				lifecycleOwner = _lifecycleOwner,
				fileUri = playerFileProvider.providesAudioFileUri(route.audioId),
				timePerPointInMs = RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE
			)
			result.onFailure { err ->
				if (err is DecoderExistsException) return@onFailure
				_uiEvents.emit(UIEvents.ShowSnackBar(err.message ?: ""))
			}
		}.launchIn(viewModelScope)
	}


	private fun updatesOnConfigChange() {
		combine(visualizer.normalizedVisualization, _clipConfigs) { visuals, configs ->
			val newVisuals = visuals.updateArrayViaConfigs(
				configs = configs,
				timeInMillisPerBar = RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE
			)
			_compressedVisualization.update { newVisuals }
		}.launchIn(viewModelScope)
	}

	override fun onCleared() {
		visualizer.cleanUp()
	}
}

