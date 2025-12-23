package com.eva.transcribe.domain

import kotlinx.coroutines.flow.Flow

interface AudioTranscriptor {

	val recognizedText: Flow<String>

	suspend fun setUp(language: LanguageModel = LanguageModel.EN_US, sampleRate: Float = 16_000f)

	suspend fun recognizeAudio(buffer: ShortArray, length: Int)

	fun cleanUp()

	companion object {
		const val MIN_RMS_TO_RECOGNIZE = 100
	}
}