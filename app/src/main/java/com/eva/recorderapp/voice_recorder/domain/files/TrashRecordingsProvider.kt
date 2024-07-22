package com.eva.recorderapp.voice_recorder.domain.files

import kotlinx.coroutines.flow.Flow

interface TrashRecordingsProvider {

	val trashedRecordingsFlow: Flow<ResourcedVoiceRecordingModels>

	suspend fun getTrashedVoiceRecordings(): ResourcedVoiceRecordingModels
}