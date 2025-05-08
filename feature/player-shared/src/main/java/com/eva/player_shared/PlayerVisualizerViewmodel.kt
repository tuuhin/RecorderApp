package com.eva.player_shared

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.eva.editor.domain.AudioConfigToActionList
import com.eva.player.data.reader.compressFloatArray
import com.eva.player.domain.AudioVisualizer
import com.eva.player_shared.util.updateArrayViaConfigs
import com.eva.ui.navigation.NavRoutes
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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerVisualizerViewmodel @Inject constructor(
	private val visualizer: AudioVisualizer,
	private val savedStateHandle: SavedStateHandle,
) : AppViewModel() {

	private val route: NavRoutes.AudioPlayer
		get() = savedStateHandle.toRoute<NavRoutes.AudioPlayer>()

	val audioId: Long
		get() = route.audioId

	private val _compressedVisualization = MutableStateFlow(floatArrayOf())

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents


	private val _clipConfigs = MutableStateFlow<AudioConfigToActionList>(emptyList())

	val fullVisualization = visualizer.normalizedVisualization
		.onStart { prepareVisuals() }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(2_000),
			initialValue = floatArrayOf()
		)

	val compressedVisuals = _compressedVisualization
		.map { it.compressFloatArray(RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE) }
		.flowOn(Dispatchers.Default)
		.onStart { updatesOnConfigChange() }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(10_000L),
			initialValue = floatArrayOf()
		)


	fun updateClipConfigs(configs: AudioConfigToActionList) = _clipConfigs.update { configs }


	private fun prepareVisuals() = viewModelScope.launch {
		val result = visualizer.prepareVisualization(
			fileId = audioId,
			timePerPointInMs = RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE
		)
		result.onFailure {
			_uiEvents.emit(UIEvents.ShowSnackBar(it.message ?: ""))
		}
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
}

