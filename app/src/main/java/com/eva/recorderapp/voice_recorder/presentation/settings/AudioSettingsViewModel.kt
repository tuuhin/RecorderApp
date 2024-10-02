package com.eva.recorderapp.voice_recorder.presentation.settings

import androidx.lifecycle.viewModelScope
import com.eva.recorderapp.common.AppViewModel
import com.eva.recorderapp.common.UIEvents
import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecorderAudioSettings
import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecorderFileSettings
import com.eva.recorderapp.voice_recorder.domain.datastore.repository.RecorderAudioSettingsRepo
import com.eva.recorderapp.voice_recorder.domain.datastore.repository.RecorderFileSettingsRepo
import com.eva.recorderapp.voice_recorder.presentation.settings.utils.AudioSettingsEvent
import com.eva.recorderapp.voice_recorder.presentation.settings.utils.FileSettingsChangeEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioSettingsViewModel @Inject constructor(
	private val audioRepo: RecorderAudioSettingsRepo,
	private val fileRepo: RecorderFileSettingsRepo,
) : AppViewModel() {

	val audioSettings = audioRepo.audioSettingsFlow
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.Lazily,
			initialValue = RecorderAudioSettings()
		)

	val fileSettings = fileRepo.fileSettingsFlow
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.Lazily,
			initialValue = RecorderFileSettings()
		)

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()


	fun onAudioEvent(event: AudioSettingsEvent) = viewModelScope.launch {
		when (event) {
			is AudioSettingsEvent.OnEncoderChange -> audioRepo.onEncoderChange(event.encoder)
			is AudioSettingsEvent.OnQualityChange -> audioRepo.onQualityChange(event.quality)
			is AudioSettingsEvent.OnSkipSilencesChange -> audioRepo.onSkipSilencesChange(event.skipAllowed)
			is AudioSettingsEvent.OnStereoModeChange -> audioRepo.onStereoModeChange(event.mode)
			is AudioSettingsEvent.OnPauseRecorderOnCalls -> {
				audioRepo.onPauseRecorderOnCallEnabled(event.isAllowed)
			}
			is AudioSettingsEvent.OnUseBluetoothMicChanged -> {
				audioRepo.onUseBluetoothMicEnabled(event.isAllowed)
			}
			is AudioSettingsEvent.OnAddLocationEnabled -> audioRepo.onAddLocationEnabled(event.isEnabled)
		}
	}

	fun onFileEvent(event: FileSettingsChangeEvent) = viewModelScope.launch {
		when (event) {
			is FileSettingsChangeEvent.OnFormatChange -> fileRepo.onFileNameFormatChange(event.format)
			is FileSettingsChangeEvent.OnRecordingPrefixChange -> fileRepo.onFilePrefixChange(event.prefix)
		}
	}
}