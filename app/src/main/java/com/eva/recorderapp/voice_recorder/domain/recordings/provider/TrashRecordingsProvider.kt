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
	 * on restore they are shown on [VoiceRecordingsProvider.getVoiceRecordingsAsResource]
	 * @param recordings a [Collection] of [TrashRecordingModel] to be recovered
	 * @return [Resource] indicating [recordings] are recovered successfully otherwise [Exception]
	 */
	suspend fun restoreRecordingsFromTrash(
		recordings: Collection<TrashRecordingModel>,
	): Resource<Unit, Exception>

	/**
	 * Creates trash entries from the original [RecordedVoiceModel], the trash recordings will not
	 * be shown directly and will be deleted with a grace period of 30 days.
	 * @see TrashRecordingsProvider
	 * @param recordings [TrashVoiceRecordings] to be trashed
	 * @return A flow emitting [Resource.Success]  everything went well, [Resource.Error] there was
	 * some security issues or other issues related to files.
	 */
	fun createTrashRecordings(recordings: Collection<RecordedVoiceModel>)
			: Flow<Resource<Collection<RecordedVoiceModel>, Exception>>


	/**
	 * Permanently deletes the trashed recordings. Remember if these are deleted they cannot be recovered anymore
	 * @param trashRecordings a [Collection] of [TrashRecordingModel] to be removed permanently
	 * @return [Resource] indicating [trashRecordings] are deleted successfully otherwise [Exception]
	 */
	suspend fun permanentlyDeleteRecordingsInTrash(trashRecordings: Collection<TrashRecordingModel>): Resource<Unit, Exception>

}