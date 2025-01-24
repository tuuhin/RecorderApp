package com.eva.recorderapp.voice_recorder.domain.recordings.provider

import android.net.Uri
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import kotlinx.coroutines.flow.Flow

typealias VoiceRecordingModels = List<RecordedVoiceModel>
typealias ResourcedVoiceRecordingModels = Resource<List<RecordedVoiceModel>, Exception>

interface VoiceRecordingsProvider {

	/**
	 * Provides a flow of recorded voice models. Settings can be configured to allow
	 * reading of recordings from other apps.
	 *
	 * @return A flow of [VoiceRecordingModels].
	 * @see getVoiceRecordings For fetching [VoiceRecordingModels] directly (without a flow).
	 * @throws Exception Exceptions are not handled within the flow. Ensure proper error handling,
	 *                   e.g., using `catch` or `try-catch` when collecting the flow.
	 */
	val voiceRecordingsFlow: Flow<VoiceRecordingModels>

	/**
	 * Provides a flow of [ResourcedVoiceRecordingModels] containing recordings owned by this app.
	 *
	 * @see RecordedVoiceModel
	 */
	val voiceRecordingsOnlyThisApp: Flow<ResourcedVoiceRecordingModels>

	/**
	 * Retrieves the current recordings. The scope of recordings retrieved (this app only or all)
	 * is determined by the [queryOthers] parameter.
	 *
	 * @param queryOthers If `true`, includes recordings from other apps; otherwise, only
	 *                           retrieves recordings owned by this app. Defaults to `false`.
	 * @return A list of [VoiceRecordingModels].
	 */
	suspend fun getVoiceRecordings(queryOthers: Boolean = false): VoiceRecordingModels

	/**
	 * Retrieves a specific  recording as a [Resource] format by its ID.
	 *
	 * @param recordingId The ID of the recording to retrieve.
	 * @return A [Resource] containing either the [RecordedVoiceModel] or an [Exception].
	 */
	suspend fun getVoiceRecordingAsResourceFromId(recordingId: Long): Resource<RecordedVoiceModel, Exception>

	/**
	 * Deletes a recording based on its URI. This method only allows deletion of recordings owned
	 * by this app.
	 *
	 * @param uri The URI of the recording to delete.
	 * @return A [Resource] indicating success (Unit) or failure (an [Exception]). Returns a
	 *         [Resource.Error] if the URI is not owned by this app.
	 */
	suspend fun deleteFileFromUri(uri: Uri): Resource<Unit, Exception>

	/**
	 * Deletes a recording based on its ID. This method only allows deletion of recordings owned
	 * by this app.
	 *
	 * @param id The ID of the recording to delete.
	 * @return A [Resource] indicating success (Unit) or failure (an [Exception]). Returns a
	 *         [Resource.Error] if the ID does not correspond to a recording owned by this app.
	 */
	suspend fun deleteFileFromId(id: Long): Resource<Unit, Exception>

	/**
	 * Permanently deletes a collection of recordings. This action is irreversible.
	 *
	 * @param recordings A collection of [RecordedVoiceModel] objects to delete.
	 * @return A [Resource] indicating success (Unit) or failure (an [Exception]).
	 */
	suspend fun permanentlyDeleteRecordedVoices(recordings: Collection<RecordedVoiceModel>): Resource<Unit, Exception>

	/**
	 * Renames a recording.
	 *
	 * @param recording The [RecordedVoiceModel] to rename.
	 * @param newName The new name for the recording.
	 * @return A flow emitting [Resource] objects indicating the success or failure of the rename operation.
	 */
	fun renameRecording(recording: RecordedVoiceModel, newName: String)
			: Flow<Resource<Unit, Exception>>

}