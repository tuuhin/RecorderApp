package com.eva.recorderapp.voice_recorder.presentation.settings.utils

import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecordQuality
import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecordingEncoders

sealed interface ChangeAudioSettingsEvent {

	data class OnAudioEncoderChange(val encoder: RecordingEncoders) : ChangeAudioSettingsEvent

	data class OnAudioQualityChange(val quality: RecordQuality) : ChangeAudioSettingsEvent

}