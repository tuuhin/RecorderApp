package com.eva.recorderapp.voice_recorder.presentation.settings

import androidx.lifecycle.viewModelScope
import com.eva.recorderapp.common.AppViewModel
import com.eva.recorderapp.common.UIEvents
import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecorderAudioSettings
import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecorderFileSettings
import com.eva.recorderapp.voice_recorder.domain.datastore.repository.RecorderAudioSettingsRepo
import com.eva.recorderapp.voice_recorder.domain.datastore.repository.RecorderFileSettingsRepo
import com.eva.recorderapp.voice_recorder.presentation.settings.utils.ChangeAudioSettingsEvent
import com.eva.recorderapp.voice_recorder.presentation.settings.utils.ChangeFileSettingsEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AudioSettingsViewModel @Inject constructor(
	private val audioSettingsRepo: RecorderAudioSettingsRepo,
	private val fileSettingsRepo: RecorderFileSettingsRepo,
) : AppViewModel() {

	val audioSettings = audioSettingsRepo.audioSettingsFlow
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.Lazily,
			initialValue = RecorderAudioSettings()
		)

	val fileSettings = fileSettingsRepo.fileSettingsFlow
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.Lazily,
			initialValue = RecorderFileSettings()
		)

	val _uiEvents = MutableSharedFlow<UIEvents>()

	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()


	fun onAudioEvent(event: ChangeAudioSettingsEvent) {

	}

	fun onFileEvent(event: ChangeFileSettingsEvent) {

	}
}