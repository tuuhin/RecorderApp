package com.eva.recordings.domain.provider


import com.eva.categories.domain.models.RecordingCategoryModel
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.recordings.domain.models.ExtraRecordingMetadataModel
import com.eva.recordings.domain.models.RecordedVoiceModel
import com.eva.utils.Resource
import kotlinx.coroutines.flow.Flow

typealias ExtraRecordingMetaDataList = Collection<ExtraRecordingMetadataModel>

interface RecordingsSecondaryDataProvider {

	/**
	 * A flow of all extra recording metadata.
	 *
	 * @return A [Flow] emitting lists of [ExtraRecordingMetaDataList].
	 */
	val providesRecordingMetaData: Flow<ExtraRecordingMetaDataList>

	/**
	 * Retrieves recording metadata as a flow for a given recording ID.
	 *
	 * @param recordingId The ID of the recording.
	 * @return A [Flow] emitting the [ExtraRecordingMetadataModel] or `null` if not found.
	 */
	fun getRecordingFromIdAsFlow(recordingId: Long): Flow<ExtraRecordingMetadataModel?>

	/**
	 * Retrieves recording metadata as a flow for a given category.
	 *
	 * @param category The [RecordingCategoryModel] to filter by.
	 * @return A [Flow] emitting lists of [ExtraRecordingMetaDataList] belonging to the specified category.
	 */
	fun recordingsFromCategory(category: RecordingCategoryModel): Flow<ExtraRecordingMetaDataList>

	/**
	 * Checks if a recording ID exists.
	 *
	 * @param recordingId The ID of the recording to check.
	 * @return `true` if the ID exists, `false` if it doesn't, or `null` if an error occurs during the check.
	 */
	suspend fun checkRecordingIdExists(recordingId: Long): Boolean?

	/**
	 * Inserts metadata for a recording.
	 *
	 * @param recordingId The ID of the recording to insert metadata for.
	 * @return A [Resource] containing the inserted [ExtraRecordingMetadataModel] on success or an [Exception] on failure.
	 */
	suspend fun insertRecordingMetaData(recordingId: Long): Resource<ExtraRecordingMetadataModel, Exception>

	/**
	 * Inserts metadata for multiple recordings in bulk.
	 *
	 * @param recordingsIds A list of recording IDs.
	 * @return A [Resource] containing `true` on successful insertion of all metadata or an [Exception] on failure.
	 */
	suspend fun insertRecordingsMetaDataBulk(recordingsIds: List<Long>): Resource<Boolean, Exception>

	/**
	 * Updates the metadata for a recording based on the provided [RecordedVoiceModel].
	 *
	 * @param model The [RecordedVoiceModel] containing the updated metadata.
	 * @return A [Resource] containing the updated [ExtraRecordingMetadataModel] on success or an [Exception] on failure.
	 */
	suspend fun updateRecordingMetaData(model: RecordedVoiceModel): Resource<ExtraRecordingMetadataModel, Exception>

	/**
	 * Updates the category for multiple recordings in bulk.
	 *
	 * @param recordingIds A list of recording IDs to update.
	 * @param category The new [RecordingCategoryModel] to assign.
	 * @return A [Resource] containing `true` on successful update of all categories or an [Exception] on failure.
	 */
	suspend fun updateRecordingCategoryBulk(
		recordingIds: List<Long>,
		category: RecordingCategoryModel,
	): Resource<Boolean, Exception>

	/**
	 * Sets the favorite status for multiple recordings in bulk.
	 *
	 * @param models A list of [VoiceRecordingModels] to update.
	 * @param isFavourite The new favorite status (`true` for favorite, `false` for not favorite). Defaults to `false`.
	 * @return A [Resource] indicating success (Unit) or failure (an [Exception]).
	 */
	suspend fun favouriteRecordingsBulk(models: VoiceRecordingModels, isFavourite: Boolean = false)
			: Resource<Unit, Exception>

	/**
	 * Deletes metadata for multiple recordings in bulk.
	 *
	 * @param models A list of [VoiceRecordingModels] for which to delete metadata.
	 * @return A [Resource] indicating success (Unit) or failure (an [Exception]).
	 */
	suspend fun deleteRecordingMetaDataBulk(models: VoiceRecordingModels): Resource<Unit, Exception>

	/**
	 * Sets the favorite status for an audio file.
	 *
	 * @param file The [AudioFileModel] to update.
	 * @param isFav The new favorite status (`true` for favorite, `false` for not favorite). Defaults to `false`.
	 * @return A [Resource] indicating success (Unit) or failure (an [Exception]).
	 */
	suspend fun favouriteAudioFile(file: AudioFileModel, isFav: Boolean = false)
			: Resource<Unit, Exception>

}