package com.eva.transcribe.domain

interface AudioTranscriptor {

	suspend fun setUp(language: LanguageModel = LanguageModel.EN_US, sampleRate: Float = 16_000f)

	suspend fun recognizeAudio(buffer: ShortArray, length: Int): TranscriptionResult?

	fun cleanUp()

	companion object {
		const val MIN_RMS_TO_RECOGNIZE = 100
	}
}