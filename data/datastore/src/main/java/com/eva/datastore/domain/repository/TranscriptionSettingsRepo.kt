package com.eva.datastore.domain.repository

import com.eva.datastore.domain.models.TranscriptionSettings
import kotlinx.coroutines.flow.Flow

interface TranscriptionSettingsRepo {

	val settingsFlow: Flow<TranscriptionSettings>

	suspend fun setting(): TranscriptionSettings

	suspend fun onEnableOrDisable(isEnabled: Boolean)

	suspend fun onUpdateModelId(modelId: String? = null)
}