package com.eva.recorderapp.voice_recorder.presentation.settings.utils

import com.eva.recorderapp.voice_recorder.domain.datastore.enums.RecordQuality
import com.eva.recorderapp.voice_recorder.domain.datastore.enums.RecordingEncoders

sealed interface AudioSettingsEvent {

	data class OnEncoderChange(val encoder: RecordingEncoders) : AudioSettingsEvent

	data class OnQualityChange(val quality: RecordQuality) : AudioSettingsEvent

	data class OnStereoModeChange(val mode: Boolean) : AudioSettingsEvent

	data class OnSkipSilencesChange(val skipAllowed: Boolean) : AudioSettingsEvent

}