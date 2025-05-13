package com.eva.feature_editor.viewmodel

import androidx.lifecycle.viewModelScope
import com.eva.editor.domain.AudioConfigToAction
import com.eva.editor.domain.AudioTransformer
import com.eva.editor.domain.EditedItemSaver
import com.eva.editor.domain.SimpleAudioPlayer
import com.eva.editor.domain.model.AudioClipConfig
import com.eva.editor.domain.model.AudioEditAction
import com.eva.feature_editor.event.EditorScreenEvent
import com.eva.feature_editor.event.TransformationState
import com.eva.feature_editor.undoredo.UndoRedoManager
import com.eva.feature_editor.undoredo.UndoRedoState
import com.eva.player.domain.model.PlayerTrackData
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.ui.viewmodel.AppViewModel
import com.eva.ui.viewmodel.UIEvents
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.time.Duration

@HiltViewModel(assistedFactory = EditorViewmodelFactory::class)
internal class AudioEditorViewModel @AssistedInject constructor(
	@Assisted private val fileModel: AudioFileModel,
	private val transformer: AudioTransformer,
	private val saver: EditedItemSaver,
	private val player: SimpleAudioPlayer,
) : AppViewModel() {

	private val _lastEditAction = MutableStateFlow(AudioEditAction.CROP)
	private val _exportFileUri = MutableStateFlow<String?>(null)

	private val _clipData = MutableStateFlow<AudioClipConfig?>(null)
	val clipConfig = _clipData.asStateFlow()

	private val _undoRedoManager = UndoRedoManager<AudioConfigToAction>(10)
	val clipConfigs = _undoRedoManager.allActions

	val undoRedoState = _undoRedoManager.undoRedoState.stateIn(
		scope = viewModelScope,
		started = SharingStarted.Eagerly,
		initialValue = UndoRedoState()
	)

	private val _exportBegin = Channel<Boolean>()
	val exportBegun = _exportBegin.consumeAsFlow()

	val transformationState = combine(
		transformer.isTransformerRunning,
		transformer.progress,
		_exportFileUri
	) { isRunning, progress, exportUri ->
		TransformationState(
			isTransforming = isRunning,
			progress = progress,
			exportFileUri = exportUri
		)
	}.stateIn(
		scope = viewModelScope,
		started = SharingStarted.WhileSubscribed(2_000L),
		initialValue = TransformationState()
	)


	val isPlayerPlaying = player.isPlaying
		.onStart {
			setPlayerItem()
			hasMediaItemChanged()
		}
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.Eagerly,
			initialValue = false
		)

	val trackData = player.trackInfoAsFlow.stateIn(
		scope = viewModelScope,
		started = SharingStarted.WhileSubscribed(1_000L),
		initialValue = PlayerTrackData(total = fileModel.duration)
	)

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents


	fun onEvent(event: EditorScreenEvent) = when (event) {
		EditorScreenEvent.PauseAudio -> viewModelScope.launch { player.pausePlayer() }
		EditorScreenEvent.PlayAudio -> viewModelScope.launch { player.startOrResumePlayer() }
		is EditorScreenEvent.OnClipConfigChange -> updateClipConfig(event.config)
		is EditorScreenEvent.OnSeekTrack -> player.onSeekDuration(event.duration)
		is EditorScreenEvent.OnEditAction -> validateAndApplyEditViaAction(event.action)
		EditorScreenEvent.BeginTransformation -> finalExport()
		EditorScreenEvent.OnDismissExportSheet -> onCancelExport()
		EditorScreenEvent.OnSaveExportFile -> onSaveExportFile()
		EditorScreenEvent.OnRedoEdit -> onUndoOrRedoConfigs(false)
		EditorScreenEvent.OnUndoEdit -> onUndoOrRedoConfigs(true)
	}

	suspend fun setPlayerItem() = player.prepareAudioFile(fileModel)

	fun hasMediaItemChanged() {
		player.isMediaItemChanged.onEach {
			val oldConfig = _clipData.getAndUpdate { null } ?: return@onEach

			// add the new pair to undo-redoManager
			val configActionPair = oldConfig to _lastEditAction.value
			_undoRedoManager.add(configActionPair)
		}.launchIn(viewModelScope)
	}


	private fun validateAndApplyEditViaAction(action: AudioEditAction) {
		viewModelScope.launch {
			val clipData = _clipData.value ?: return@launch
			val trackData = trackData.value

			if (clipData.start == Duration.ZERO && clipData.end == trackData.total) {
				val message = when (action) {
					AudioEditAction.CROP -> "Crop section is same as original"
					AudioEditAction.CUT -> "Cannot remove the whole media"
				}
				_uiEvents.emit(UIEvents.ShowSnackBar(message))
				return@launch
			}

			if (!clipData.hasMinimumDuration) {
				val message = "Editor needs a ${AudioClipConfig.MIN_CLIP_DURATION} clip"
				_uiEvents.emit(UIEvents.ShowSnackBar(message))
				return@launch
			}

			val lastAction = _lastEditAction.updateAndGet { action }
			// new clipping config
			val clippingData = _undoRedoManager.allActions.value + (clipData to lastAction)

			val result = player.editMediaPortions(fileModel, clippingData)
			result.fold(
				onSuccess = {
					val message = when (action) {
						AudioEditAction.CROP -> "Crop Applied"
						AudioEditAction.CUT -> "Cut Applied"
					}
					_uiEvents.emit(UIEvents.ShowToast(message))
				},
				onFailure = { _uiEvents.emit(UIEvents.ShowSnackBar(it.message ?: "Some error")) },
			)
		}
	}

	fun onUndoOrRedoConfigs(isUndo: Boolean) {
		viewModelScope.launch {
			// new clipping config
			val clippingData = if (isUndo) _undoRedoManager.undo()
			else _undoRedoManager.redo()

			val filteredData = clippingData.filter { (config, _) -> config.hasMinimumDuration }

			val result = player.editMediaPortions(fileModel, filteredData)
			result.fold(
				onSuccess = {},
				onFailure = { _uiEvents.emit(UIEvents.ShowSnackBar(it.message ?: "Some error")) },
			)
		}
	}


	private fun updateClipConfig(clipConfig: AudioClipConfig) {
		val track = trackData.value
		val clipData = _clipData.updateAndGet { clipConfig } ?: return
		if (track.current in clipData.start..clipData.end) return

		if (!clipData.hasMinimumDuration) {
			val message = "Editor needs a ${AudioClipConfig.MIN_CLIP_DURATION} clip"
			viewModelScope.launch { _uiEvents.emit(UIEvents.ShowSnackBar(message)) }
		}

		val seekDuration = with(trackData.value) {
			val distanceToStart =
				abs(current.inWholeMilliseconds - clipData.start.inWholeMilliseconds)
			val distanceToEnd = abs(current.inWholeMilliseconds - clipData.end.inWholeMilliseconds)
			if (distanceToEnd < distanceToStart) clipData.end else clipData.start
		}

		player.onSeekDuration(seekDuration)
	}


	private fun onSaveExportFile() {
		val fileUri = _exportFileUri.value ?: return
		saver.saveItem(fileModel, fileUri)
		// will trigger a navigation event to recordings screen
		viewModelScope.launch { _exportBegin.send(true) }
	}

	private fun onCancelExport() = viewModelScope.launch {
		val fileUri = _exportFileUri.getAndUpdate { null } ?: return@launch
		transformer.removeTransformsFile(fileUri)
	}

	private fun finalExport() = viewModelScope.launch {

		val filterValidConfigs = _undoRedoManager.allActions.value
			.filter { (config, _) -> config.hasMinimumDuration }

		val result = transformer.transformAudio(fileModel, filterValidConfigs)
		result.fold(
			onSuccess = { data -> _exportFileUri.update { data } },
			onFailure = { exp ->
				val message = exp.message ?: "Some transformation error"
				_uiEvents.emit(UIEvents.ShowToast(message))
			},
		)
	}


	override fun onCleared() {
		player.cleanUp()
		_undoRedoManager.clearHistory()
		super.onCleared()
	}
}