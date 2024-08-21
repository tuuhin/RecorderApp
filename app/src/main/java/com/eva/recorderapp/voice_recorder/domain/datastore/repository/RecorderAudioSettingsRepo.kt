package com.eva.recorderapp.voice_recorder.domain.datastore.repository

import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecordQuality
import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecorderAudioSettings
import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecordingEncoders
import kotlinx.coroutines.flow.Flow

interface RecorderAudioSettingsRepo {

	val audioSettingsFlow: Flow<RecorderAudioSettings>

	val audioSettings: RecorderAudioSettings

	suspend fun onEncoderChange(encoder: RecordingEncoders)

	suspend fun onQualityChange(quality: RecordQuality)

	suspend fun onStereoModeChange(mode: Boolean)

	suspend fun onSkipSilencesChange(skipAllowed: Boolean)
}