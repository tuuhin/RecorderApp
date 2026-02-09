package com.eva.transcribe.data

import com.eva.transcribe.domain.models.LanguageModel

internal val LanguageModel.downloadURI: String
	get() = when (this) {
		LanguageModel.EN_US -> "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
		LanguageModel.EN_IN -> "https://alphacephei.com/vosk/models/vosk-model-small-en-in-0.4.zip"
	}

internal val LanguageModel.savedModelPath: String
	get() = when (this) {
		LanguageModel.EN_US -> "models/vosk-model-small-en-us-0.15"
		LanguageModel.EN_IN -> "models/vosk-model-small-en-in-0.4"
	}