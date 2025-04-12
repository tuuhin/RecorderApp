package com.eva.datastore.domain.repository

import com.eva.datastore.domain.enums.RecordQuality
import com.eva.datastore.domain.enums.RecordingEncoders
import com.eva.datastore.domain.models.RecorderAudioSettings
import kotlinx.coroutines.flow.Flow

interface RecorderAudioSettingsRepo {

	val audioSettingsFlow: Flow<RecorderAudioSettings>

	val audioSettings: RecorderAudioSettings

	suspend fun onEncoderChange(encoder: RecordingEncoders)

	suspend fun onQualityChange(quality: RecordQuality)

	suspend fun onStereoModeChange(mode: Boolean)

	suspend fun onSkipSilencesChange(skipAllowed: Boolean)

	suspend fun onUseBluetoothMicEnabled(isAllowed: Boolean)

	suspend fun onPauseRecorderOnCallEnabled(isEnabled: Boolean)

	suspend fun onAddLocationEnabled(isEnabled: Boolean)
}