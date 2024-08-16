package com.eva.recorderapp.voice_recorder.domain.datastore.repository

import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecorderAudioSettings
import kotlinx.coroutines.flow.Flow

interface RecorderAudioSettingsRepo {

	val audioSettingsFlow: Flow<RecorderAudioSettings>

	val audioSettings: RecorderAudioSettings
}