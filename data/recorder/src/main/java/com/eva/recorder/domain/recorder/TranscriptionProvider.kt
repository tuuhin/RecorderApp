package com.eva.recorder.domain.recorder

import kotlinx.coroutines.flow.Flow

interface TranscriptionProvider {

	val transcription: Flow<String>

	suspend fun initTranscriptions()

	fun transcriptCleanUp()
}