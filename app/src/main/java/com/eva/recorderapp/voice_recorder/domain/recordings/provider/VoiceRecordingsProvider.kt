package com.eva.recorderapp.voice_recorder.domain.recordings.provider

import android.net.Uri
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import kotlinx.coroutines.flow.Flow

typealias VoiceRecordingModels = List<RecordedVoiceModel>
typealias ResourcedVoiceRecordingModels = Resource<List<RecordedVoiceModel>, Exception>

interface VoiceRecordingsProvider {

	/**
	 * A flow version of [getVoiceRecordings]
	 * @return a flow version of [ResourcedVoiceRecordingModels]
	 * @see getVoiceRecordings its the base function which is only turned into a flow
	 */
	val voiceRecordingsFlow: Flow<ResourcedVoiceRecordingModels>

	/**
	 * Gets the currently saved recordings from the storage
	 * @return [Resource.Success] of [VoiceRecordingModels] if everything goes well otherwise [Resource.Error]
	 */
	suspend fun getVoiceRecordings(): ResourcedVoiceRecordingModels

	/**
	 * Deleted the current recording from the given uri, tries to delete
	 * the values associated with the uri, the uri's owner package should be
	 * this app , otherwise  a [Resource.Error]
	 */
	suspend fun deleteFileFromUri(uri: Uri): Resource<Boolean, Exception>

	/**
	 * Same as [deleteFileFromUri] just rather than submitting the uri to be deleted we pass
	 * the id for the entry. Again the [id] of the matching uri's owner package should be this one
	 * otherwise [Resource.Error]
	 */
	suspend fun deleteFileFromId(id: Long): Resource<Boolean, Exception>

	/**
	 * Permanently deletes recordings. Remember if these are deleted they cannot be recoverd any more
	 * @param recordings a [Collection] of [RecordedVoiceModel] to be removed permanently
	 * @return [Resource] indicating [recordings] are deleted successfully or there is any error
	 */
	suspend fun permanentlyDeleteRecordedVoices(recordings: Collection<RecordedVoiceModel>): Resource<Unit, Exception>

	/**
	 * Renames the previous recording to a new name
	 * @param recording Thre [RecordedVoiceModel] whoes name need to be changed
	 * @param newName New name for the file
	 * @returna flow indicating everything performed well
	 */
	suspend fun renameRecording(
		recording: RecordedVoiceModel,
		newName: String
	): Flow<Resource<Boolean, Exception>>

}