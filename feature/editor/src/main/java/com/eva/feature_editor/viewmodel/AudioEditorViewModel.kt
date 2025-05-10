package com.eva.feature_editor.viewmodel

import androidx.lifecycle.viewModelScope
import com.eva.editor.domain.AudioConfigToActionList
import com.eva.editor.domain.AudioTransformer
import com.eva.editor.domain.EditedItemSaver
import com.eva.editor.domain.SimpleAudioPlayer
import com.eva.editor.domain.model.AudioClipConfig
import com.eva.editor.domain.model.AudioEditAction
import com.eva.feature_editor.event.EditorScreenEvent
import com.eva.feature_editor.event.TransformationState
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

	private val _clipData = MutableStateFlow<AudioClipConfig?>(null)
	val clipConfig = _clipData.asStateFlow()

	private val _allConfigs = MutableStateFlow<AudioConfigToActionList>(emptyList())
	val clipConfigs = _allConfigs.asStateFlow()

	private val _lastEditAction = MutableStateFlow(AudioEditAction.CROP)
	private val _exportFileUri = MutableStateFlow<String?>(null)

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
		EditorScreenEvent.CropSelectedArea -> validateAndCropSection()
		EditorScreenEvent.RemoveSelectedArea -> validateAndCutSection()
		EditorScreenEvent.BeginTransformation -> finalExport()
		EditorScreenEvent.OnDismissExportSheet -> _exportFileUri.update { null }
		EditorScreenEvent.OnSaveExportFile -> onSaveExportFile()
	}

	suspend fun setPlayerItem() = player.prepareAudioFile(fileModel)

	fun hasMediaItemChanged() = player.isMediaItemChanged.onEach {
		val oldConfig = _clipData.getAndUpdate { null } ?: return@onEach
		_allConfigs.update { it + (oldConfig to _lastEditAction.value) }
	}.launchIn(viewModelScope)


	private fun validateAndCutSection() = viewModelScope.launch {
		val clipData = _clipData.value ?: return@launch
		val trackData = trackData.value

		if (clipData.start == Duration.ZERO && clipData.end == trackData.total) {
			_uiEvents.emit(UIEvents.ShowSnackBar("Cannot remove the whole media"))
			return@launch
		}

		_lastEditAction.update { AudioEditAction.CUT }
		val result = player.cutMediaPortion(fileModel, clipData)
		result.fold(
			onSuccess = { _uiEvents.emit(UIEvents.ShowToast("Section Removed")) },
			onFailure = { _uiEvents.emit(UIEvents.ShowSnackBar(it.message ?: "Some error")) },
		)
	}

	private fun validateAndCropSection() = viewModelScope.launch {
		val clipData = _clipData.value ?: return@launch
		val trackData = trackData.value

		if (clipData.start == Duration.ZERO && clipData.end == trackData.total) {
			_uiEvents.emit(UIEvents.ShowSnackBar("Crop section is same as original"))
			return@launch
		}

		_lastEditAction.update { AudioEditAction.CROP }
		val result = player.cropMediaPortion(fileModel, clipData)
		result.fold(
			onSuccess = { _uiEvents.emit(UIEvents.ShowToast("Section Cropped")) },
			onFailure = { _uiEvents.emit(UIEvents.ShowSnackBar(it.message ?: "Some error")) },
		)
	}


	private fun updateClipConfig(clipConfig: AudioClipConfig) {
		val track = trackData.value
		val newClip = _clipData.updateAndGet { clipConfig } ?: return
		if (track.current in newClip.start..newClip.end) return

		val seekDuration = with(trackData.value) {
			val distanceToStart =
				abs(current.inWholeMilliseconds - newClip.start.inWholeMilliseconds)
			val distanceToEnd = abs(current.inWholeMilliseconds - newClip.end.inWholeMilliseconds)
			if (distanceToEnd < distanceToStart) newClip.end else newClip.start
		}

		player.onSeekDuration(seekDuration)
	}


	private fun onSaveExportFile() {
		val fileUri = _exportFileUri.value ?: return
		saver.saveItem(fileModel, fileUri)
		viewModelScope.launch { _exportBegin.send(true) }
	}

	private fun finalExport() = viewModelScope.launch {

		val filterValidConfigs = _allConfigs.value
			.filter { (config, _) -> config.hasMinimumDuration }
// TODO: If the export is cancelled delete the file
		val result = transformer.transformAudio(fileModel, filterValidConfigs)
		result.fold(
			onSuccess = { data -> _exportFileUri.update { data } },
			onFailure = {
				_uiEvents.emit(UIEvents.ShowSnackBar(it.message ?: ""))
			},
		)
	}


	override fun onCleared() {
		player.cleanUp()
		super.onCleared()
	}
}