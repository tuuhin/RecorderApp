package com.eva.transcribe.domain

import kotlinx.coroutines.flow.Flow

interface AudioTranscriptor {

	val recognizedText: Flow<String>

	suspend fun setUp(language: LanguageModel = LanguageModel.EN_US)

	suspend fun recognizeAudio(buffer: ShortArray, length: Int)

	fun cleanUp()
}