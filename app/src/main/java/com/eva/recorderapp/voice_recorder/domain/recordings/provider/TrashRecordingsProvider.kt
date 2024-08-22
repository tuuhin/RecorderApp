package com.eva.recorderapp.voice_recorder.domain.recordings.provider

import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.domain.recordings.models.TrashRecordingModel
import kotlinx.coroutines.flow.Flow

typealias TrashVoiceRecordings = List<TrashRecordingModel>
typealias ResourcedTrashRecordingModels = Resource<List<TrashRecordingModel>, Exception>


interface TrashRecordingsProvider {

	/**
	 * A flow version for [getTrashedVoiceRecordings]
	 * @return A [Flow] of [ResourcedTrashRecordingModels]
	 * @see getTrashedVoiceRecordings
	 */
	val trashedRecordingsFlow: Flow<ResourcedTrashRecordingModels>

	/**
	 * Gets the current trashed recordings for this app
	 * @return [Resource.Success] indicating [TrashVoiceRecordings] otherwise [Resource.Error]
	 */
	suspend fun getTrashedVoiceRecordings(): ResourcedTrashRecordingModels

	/**
	 * Restore the original [RecordedVoiceModel], from the trash for one to one mapping of [TrashRecordingModel]
	 * on restore they are shown on [VoiceRecordingsProvider.getVoiceRecordings]
	 * @param trashRecordings a [Collection] of [TrashRecordingModell] to be recovered
	 * @return [Resource] indicating [recordings] are recovered successfully otherwise [Exception]
	 */
	suspend fun restoreRecordingsFromTrash(
		trashRecordings: Collection<TrashRecordingModel>
	): Resource<Unit, Exception>

	/**
	 * Creates a trash entry from the original [RecordedVoiceModel], the trash recordings will not
	 * show directly on normal recordings to get trash recordings follow [getTrashedVoiceRecordings]
	 * @param recordings a [Collection] of [RecordedVoiceModel] to be trashed
	 * @return [Resource] indicating [recordings] are moved to trash successfully otherwise [Exception]
	 */
	suspend fun createTrashRecordings(
		recordings: Collection<RecordedVoiceModel>
	): Resource<Unit, Exception>

	/**
	 * Permanently deletes the trashed recordings. Remember if these are deleted they cannot be recoverd any more
	 * @param trashRecordings a [Collection] of [TrashRecordingModel] to be removed permanently
	 * @return [Resource] indicating [trashRecordings] are deleted successfully otherwise [Exception]
	 */
	suspend fun permanentlyDeleteRecordedVoicesInTrash(
		trashRecordings: Collection<TrashRecordingModel>
	): Resource<Unit, Exception>

}