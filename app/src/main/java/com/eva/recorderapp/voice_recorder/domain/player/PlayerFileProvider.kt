package com.eva.recorderapp.voice_recorder.domain.player

import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.models.AudioFileModel
import kotlinx.coroutines.flow.Flow

typealias ResourcedDetailedRecordingModel = Resource<AudioFileModel, Exception>

interface PlayerFileProvider {

	fun getAudioFileInfo(id: Long): Flow<ResourcedDetailedRecordingModel>

	suspend fun getPlayerInfoFromAudioId(id: Long): ResourcedDetailedRecordingModel
}