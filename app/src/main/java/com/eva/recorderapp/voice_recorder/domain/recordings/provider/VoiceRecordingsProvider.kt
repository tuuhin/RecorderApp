package com.eva.recorderapp.voice_recorder.domain.recordings.provider

import android.net.Uri
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import kotlinx.coroutines.flow.Flow

typealias VoiceRecordingModels = List<RecordedVoiceModel>
typealias ResourcedVoiceRecordingModels = Resource<List<RecordedVoiceModel>, Exception>

interface VoiceRecordingsProvider {

	/**
	 * A flow of recorded voice models,
	 * @return a flow version of [ResourcedVoiceRecordingModels]
	 * @see getVoiceRecordings to fetch [VoiceRecordingModels] normally use this.
	 * @throws Exception Make sure you check for any exceptions
	 */
	val voiceRecordingsFlow: Flow<VoiceRecordingModels>

	/**
	 * A resourced version of the [voiceRecordingsFlow].[Exception]'s are wrapped so no need
	 * worry about exceptions
	 */
	val voiceRecordingFlowAsResource: Flow<ResourcedVoiceRecordingModels>


	suspend fun getVoiceRecordings(): VoiceRecordingModels

	/**
	 * Gets the currently saved recordings from the storage
	 * @return [Resource.Success] of [VoiceRecordingModels] if everything goes well otherwise [Resource.Error]
	 */
	suspend fun getVoiceRecordingsAsResource(): ResourcedVoiceRecordingModels


	suspend fun getVoiceRecordingAsResourceFromId(recordingId: Long): Resource<RecordedVoiceModel, Exception>

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
	 * Permanently deletes recordings. Remember if these are deleted they cannot be recovered anymore
	 * @param recordings a [Collection] of [RecordedVoiceModel] to be removed permanently
	 * @return [Resource] indicating [recordings] are deleted successfully or there is any error
	 */
	suspend fun permanentlyDeleteRecordedVoices(recordings: Collection<RecordedVoiceModel>): Resource<Unit, Exception>

	/**
	 * Renames the previous recording to a new name
	 * @param recording Three [RecordedVoiceModel] whose name need to be changed
	 * @param newName New name for the file
	 * @return a flow indicating everything performed well
	 */
	fun renameRecording(recording: RecordedVoiceModel, newName: String)
			: Flow<Resource<Boolean, Exception>>

}