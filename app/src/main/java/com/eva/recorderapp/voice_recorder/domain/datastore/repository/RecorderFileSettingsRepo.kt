package com.eva.recorderapp.voice_recorder.domain.datastore.repository

import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecorderFileSettings
import kotlinx.coroutines.flow.Flow

interface RecorderFileSettingsRepo {

	val fileSettingsFlow: Flow<RecorderFileSettings>

	val fileSettings: RecorderFileSettings
}