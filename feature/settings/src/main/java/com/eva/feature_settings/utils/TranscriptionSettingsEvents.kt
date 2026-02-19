package com.eva.feature_settings.utils

import com.eva.transcribe.domain.models.STTModel

sealed interface TranscriptionSettingsEvents {
	data object OnToggleTranscriptions : TranscriptionSettingsEvents
	data class OnStartModelDownload(val model: STTModel) : TranscriptionSettingsEvents
	data class OnDeleteDownloadedModel(val model: STTModel) : TranscriptionSettingsEvents
	data class OnSelectModel(val model: STTModel? = null) : TranscriptionSettingsEvents
	data class OnSelectInvalidModel(val model: STTModel) : TranscriptionSettingsEvents
}