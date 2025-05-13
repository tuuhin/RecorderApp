package com.eva.recordings.domain.provider

import com.eva.recordings.domain.models.AudioFileModel
import com.eva.utils.Resource
import kotlinx.coroutines.flow.Flow

typealias ResourcedDetailedRecordingModel = Resource<AudioFileModel, Exception>

interface PlayerFileProvider {

	fun providesAudioFileUri(audioId: Long): String

	fun getAudioFileFromIdFlow(id: Long): Flow<ResourcedDetailedRecordingModel>

	suspend fun getAudioFileFromId(id: Long): ResourcedDetailedRecordingModel
}