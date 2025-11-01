package com.eva.recordings.domain.provider

import com.eva.recordings.domain.models.RecordedVoiceModel
import com.eva.recordings.domain.models.TrashRecordingModel
import com.eva.utils.Resource
import kotlinx.coroutines.flow.Flow

typealias TrashVoiceRecordings = List<TrashRecordingModel>
typealias ResourcedTrashRecordingModels = Resource<List<TrashRecordingModel>, Exception>


interface TrashRecordingsProvider {

	/**
	 * Provides a flow of trashed recordings.
	 *
	 * @return A [Flow] of [ResourcedTrashRecordingModels].
	 * @see getTrashedVoiceRecordings For retrieving trashed recordings directly (without a flow).
	 */
	val trashedRecordingsFlow: Flow<ResourcedTrashRecordingModels>

	/**
	 * Retrieves the current trashed recordings for this app.
	 *
	 * @return A [ResourcedTrashRecordingModels] containing either a [Resource.Success] with the
	 *         [TrashVoiceRecordings] or a [Resource.Error] with an [Exception].
	 */
	suspend fun getTrashedVoiceRecordings(): ResourcedTrashRecordingModels

	/**
	 * Restores recordings from the trash. This attempts to restore the original
	 * [RecordedVoiceModel] for each provided [TrashRecordingModel].
	 *
	 * @param recordings A [Collection] of [TrashRecordingModel] objects to restore.
	 * @return A [Resource] indicating success (Unit) or failure (an [Exception]).
	 */
	suspend fun restoreRecordingsFromTrash(
		recordings: Collection<TrashRecordingModel>,
	): Resource<Unit, Exception>

	/**
	 * Creates trash entries for the provided [RecordedVoiceModel] objects. These trashed
	 * recordings will not be immediately visible and are subject to a grace period
	 * (e.g., 30 days) before permanent deletion.
	 *
	 * @param recordings A [Collection] of [RecordedVoiceModel] objects to move to the trash.
	 * @return A flow emitting [Resource] objects. A [Resource.Success] indicates that the
	 *         trash entries were created successfully. A [Resource.Error] indicates a
	 *         security issue or other file-related error. data is kept for models to handle the
	 *         exceptions if needed.
	 */
	fun createTrashRecordings(recordings: Collection<RecordedVoiceModel>)
			: Flow<Resource<Collection<RecordedVoiceModel>, Exception>>


	/**
	 * Permanently deletes recordings from the trash. This action is irreversible.
	 *
	 * @param trashRecordings A [Collection] of [TrashRecordingModel] objects to permanently delete.
	 * @return [Flow] of [ResourcedTrashRecordingModels] As there can be security exceptions for certain files it's a flow resources to indicate
	 * few have succeeded and few caught exceptions
	 */
	fun permanentlyDeleteRecordingsInTrash(trashRecordings: List<TrashRecordingModel>): Flow<Resource<List<TrashRecordingModel>, Exception>>

	/**
	 * Permanently deletes recordings from the trash. This action is irreversible.
	 *
	 * @param trashRecordings A [Collection] of [TrashRecordingModel] objects to permanently delete,
	 * If the owner is now this app then the [trashRecordings] are ignored
	 * @return [Resource] Indicating the recordings has been deleted
	 *
	 */
	suspend fun permanentlyDeleteRecordings(trashRecordings: List<TrashRecordingModel>): Resource<Unit, Exception>
}