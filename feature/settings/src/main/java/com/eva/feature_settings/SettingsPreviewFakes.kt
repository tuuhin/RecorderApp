package com.eva.feature_settings

import com.eva.transcribe.domain.models.STTModel
import com.eva.transcribe.domain.models.STTModelState

internal object SettingsPreviewFakes {

	val STT_MODELS_LIST = listOf(
		STTModel(
			modelId = "Custom model 01",
			locale = "en-us",
			downloadURI = "",
			downloadedVersion = "1.0.0",
			state = STTModelState.UN_AVAILABLE
		),
		STTModel(
			modelId = "Custom model 02",
			locale = "en-in",
			downloadURI = "",
			downloadedVersion = "1.0.0",
			state = STTModelState.DOWNLOADING
		),
		STTModel(
			modelId = "Custom model 03",
			locale = "bn",
			downloadURI = "",
			downloadedVersion = "1.0.0",
			state = STTModelState.DOWNLOADED
		)
	)

	val STT_MODEL = STTModel(
		modelId = "Custom model 01",
		locale = "en-us",
		downloadURI = "",
		downloadedVersion = "1.0.0",
		state = STTModelState.UN_AVAILABLE
	)
}