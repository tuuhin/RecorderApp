package com.eva.recorderapp.voice_recorder.presentation.settings.utils

import com.eva.recorderapp.voice_recorder.domain.datastore.enums.RecordQuality
import com.eva.recorderapp.voice_recorder.domain.datastore.enums.RecordingEncoders

sealed interface AudioSettingsEvent {

	data class OnEncoderChange(val encoder: RecordingEncoders) : AudioSettingsEvent

	data class OnQualityChange(val quality: RecordQuality) : AudioSettingsEvent

	data class OnStereoModeChange(val mode: Boolean) : AudioSettingsEvent

	data class OnSkipSilencesChange(val skipAllowed: Boolean) : AudioSettingsEvent

	data class OnPauseRecorderOnCalls(val isAllowed: Boolean) : AudioSettingsEvent

	data class OnUseBluetoothMicChanged(val isAllowed: Boolean) : AudioSettingsEvent

	data class OnAddLocationEnabled(val isEnabled: Boolean) : AudioSettingsEvent

}