package com.eva.recorderapp.voice_recorder.presentation.settings

import androidx.lifecycle.viewModelScope
import com.eva.recorderapp.common.AppViewModel
import com.eva.recorderapp.common.UIEvents
import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecorderSettings
import com.eva.recorderapp.voice_recorder.domain.datastore.repository.RecorderSettingsRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AudioSettingsViewModel @Inject constructor(
	private val settingsRepo: RecorderSettingsRepo
) : AppViewModel() {

	val settings = settingsRepo.recorderSettingsAsFlow.stateIn(
		scope = viewModelScope,
		started = SharingStarted.Eagerly,
		initialValue = RecorderSettings()
	)

	val _uiEvents = MutableSharedFlow<UIEvents>()

	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()


	fun onEvent(event: ChangeSettingsEvent) {

	}
}