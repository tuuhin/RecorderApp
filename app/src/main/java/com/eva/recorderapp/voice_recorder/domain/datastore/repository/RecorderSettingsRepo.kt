package com.eva.recorderapp.voice_recorder.domain.datastore.repository

import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecorderSettings
import kotlinx.coroutines.flow.Flow

interface RecorderSettingsRepo {

	val recorderSettingsAsFlow: Flow<RecorderSettings>

	val recorderSettings: RecorderSettings
}