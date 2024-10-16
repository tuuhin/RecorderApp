package com.eva.recorderapp.voice_recorder.domain.recordings.provider

import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.categories.models.RecordingCategoryModel
import com.eva.recorderapp.voice_recorder.domain.player.model.AudioFileModel
import com.eva.recorderapp.voice_recorder.domain.recordings.models.ExtraRecordingMetadataModel
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import kotlinx.coroutines.flow.Flow

typealias ExtraRecordingMetaDataList = Collection<ExtraRecordingMetadataModel>

interface RecordingsSecondaryDataProvider {

	val providesRecordingMetaData: Flow<ExtraRecordingMetaDataList>

	fun getRecordingFromIdAsFlow(recordingId: Long): Flow<ExtraRecordingMetadataModel?>

	fun recordingsFromCategory(category: RecordingCategoryModel): Flow<ExtraRecordingMetaDataList>

	suspend fun checkRecordingIdExists(recordingId: Long): Boolean?

	suspend fun insertRecordingMetaData(recordingId: Long): Resource<ExtraRecordingMetadataModel, Exception>

	suspend fun insertRecordingsMetaDataBulk(recordingsIds: List<Long>): Resource<Boolean, Exception>

	suspend fun updateRecordingMetaData(model: RecordedVoiceModel): Resource<ExtraRecordingMetadataModel, Exception>

	suspend fun updateRecordingCategoryBulk(
		recordingIds: List<Long>,
		category: RecordingCategoryModel,
	): Resource<Boolean, Exception>

	suspend fun favouriteRecordingsBulk(models: VoiceRecordingModels, isFavourite: Boolean = false)
			: Resource<Unit, Exception>

	suspend fun deleteRecordingMetaDataBulk(models: VoiceRecordingModels): Resource<Unit, Exception>

	suspend fun favouriteAudioFile(file: AudioFileModel, isFav: Boolean = false)
			: Resource<Unit, Exception>

}