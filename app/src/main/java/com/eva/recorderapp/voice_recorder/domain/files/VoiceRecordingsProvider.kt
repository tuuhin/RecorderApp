package com.eva.recorderapp.voice_recorder.domain.files

import android.net.Uri
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.models.RecordedVoiceModel
import kotlinx.coroutines.flow.Flow

typealias VoiceRecordingModels = List<RecordedVoiceModel>
typealias ResourcedVoiceRecordingModels = Resource<List<RecordedVoiceModel>, Exception>

interface VoiceRecordingsProvider {

	val voiceRecordingsFlow: Flow<ResourcedVoiceRecordingModels>

	suspend fun getVoiceRecordings(): ResourcedVoiceRecordingModels

	suspend fun deleteFileFromUri(uri: Uri): Resource<Boolean, Exception>

	suspend fun deleteFileFromId(id: Long): Resource<Boolean, Exception>

	suspend fun createTrashRecordings(models: Collection<RecordedVoiceModel>): Resource<Unit, Exception>
}