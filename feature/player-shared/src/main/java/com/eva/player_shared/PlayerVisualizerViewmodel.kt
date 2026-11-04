package com.eva.player_shared

import androidx.lifecycle.viewModelScope
import com.com.visualizer.data.ext.compressFloatArray
import com.com.visualizer.domain.AudioVisualizer
import com.com.visualizer.domain.VisualizationData
import com.com.visualizer.domain.VisualizerSession
import com.com.visualizer.domain.VisualizerState
import com.eva.editor.domain.AudioConfigToActionList
import com.eva.player_shared.factory.PlayerVisualizerViewmodelFactory
import com.eva.player_shared.util.CoroutineLifecycleOwner
import com.eva.player_shared.util.updateArrayViaConfigs
import com.eva.recordings.domain.provider.PlayerFileProvider
import com.eva.ui.viewmodel.AppViewModel
import com.eva.ui.viewmodel.UIEvents
import com.eva.utils.RecorderConstants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = PlayerVisualizerViewmodelFactory::class)
class PlayerVisualizerViewmodel @AssistedInject constructor(
    @Assisted private val audioId: Long,
    private val visualizer: AudioVisualizer,
    private val playerFileProvider: PlayerFileProvider,
) : AppViewModel() {

    private val _lifecycleOwner by lazy { CoroutineLifecycleOwner(viewModelScope.coroutineContext) }
    private val _visualizerToken by lazy { VisualizerSession(audioId) }

    private val _clipConfigs = MutableStateFlow<AudioConfigToActionList>(emptyList())

    override val uiEvent: SharedFlow<UIEvents>
        field = MutableSharedFlow<UIEvents>()

    private var _prepareVisualsJob: Job? = null
    private var _isFailureReported = false

    private val visualizationData: StateFlow<VisualizationData> by lazy {
        visualizer.observeVisuals(_visualizerToken)
            .onStart { observeVisualizerState() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = VisualizationData(),
            )
    }

    val fullVisualization = visualizationData
        .map { it.values }
        .onStart { prepareVisuals() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = floatArrayOf(),
        )

    val compressedVisuals = combine(fullVisualization, _clipConfigs, ::computeCompactVisuals)
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = floatArrayOf(),
        )

    val isVisualsReady = visualizationData
        .map { data -> data.state == VisualizerState.FINISHED && data.values.isNotEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    fun updateClipConfigs(configs: AudioConfigToActionList) = _clipConfigs.update { configs }


    private fun prepareVisuals() {
        if (_prepareVisualsJob?.isActive == true) return

        _prepareVisualsJob = viewModelScope.launch {
            // read in the current state
            val current = visualizer.observeVisuals(_visualizerToken).firstOrNull()

            // if current is running or its finished and no values
            val currentRunning = current != null && current.state == VisualizerState.RUNNING
            val currentFinishedAndEmpty =
                current != null && current.state == VisualizerState.FINISHED && current.values.isNotEmpty()

            // skip in these cases
            if (currentRunning || currentFinishedAndEmpty) return@launch

            // is failure reset
            _isFailureReported = false

            // load in the file uri and visuals
            playerFileProvider.providesAudioFileUri(audioId).fold(
                onSuccess = { fileURI ->
                    visualizer.prepareVisualization(
                        lifecycleOwner = _lifecycleOwner,
                        session = _visualizerToken,
                        fileUri = fileURI,
                        timePerPointInMs = RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE,
                    ).onFailure { err ->
                        reportPreparationFailure(err.message)
                    }
                },
                onFailure = { err ->
                    reportPreparationFailure(err.message)
                },
            )
        }
    }

    private suspend fun computeCompactVisuals(visuals: FloatArray, configs: AudioConfigToActionList): FloatArray {
        if (visuals.isEmpty()) return floatArrayOf()
        return try {
            // compressed visuals
            visuals.updateArrayViaConfigs(configs, RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE)
                .compressFloatArray(RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE)
        } catch (_: IllegalStateException) {
            uiEvent.emit(UIEvents.ShowSnackBar("Failed to compute visuals"))
            floatArrayOf()
        }
    }

    private fun observeVisualizerState() {
        // if we get any error state report it to show a snackbar
        visualizer.observeVisuals(_visualizerToken).map { it.state }
            .distinctUntilChanged()
            .filter { state -> state == VisualizerState.FAILED }
            .onEach { reportPreparationFailure() }
            .launchIn(viewModelScope)
    }

    private fun reportPreparationFailure(message: String? = null) = viewModelScope.launch {

        if (_isFailureReported) return@launch
        _isFailureReported = true

        uiEvent.emit(
            UIEvents.ShowSnackBarWithActions(
                message = message ?: "Failed to load visualization data",
                actionText = "Retry",
                action = ::retryVisualization,
            ),
        )
    }

    private fun retryVisualization() {
        // clear the job
        _prepareVisualsJob?.cancel()
        _prepareVisualsJob = null
        // prepare visuals
        prepareVisuals()
    }

    override fun onCleared() {
        // clear the visuals job
        _prepareVisualsJob?.cancel()
        _prepareVisualsJob = null
        // clean up the visualizer
        visualizer.cleanUp(_visualizerToken)
    }
}
