package com.eva.transcribe.domain

import com.eva.transcribe.domain.models.TranscriptionResult

interface AudioTranscriptor {

	suspend fun setUp(modelId: String, sampleRate: Float = 16_000f): Result<Unit>

	suspend fun recognizeAudio(buffer: ShortArray, length: Int): TranscriptionResult?

	fun cleanUp()

	companion object {
		const val MIN_RMS_TO_RECOGNIZE = 100
	}
}