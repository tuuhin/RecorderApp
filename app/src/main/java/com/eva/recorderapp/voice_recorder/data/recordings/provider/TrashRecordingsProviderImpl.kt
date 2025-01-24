package com.eva.recorderapp.voice_recorder.data.recordings.provider

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.database.SQLException
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import com.eva.recorderapp.R
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.data.database.dao.RecordingsMetadataDao
import com.eva.recorderapp.voice_recorder.data.database.entity.RecordingsMetaDataEntity
import com.eva.recorderapp.voice_recorder.domain.recordings.exceptions.CannotTrashFileDifferentOwnerException
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.domain.recordings.models.TrashRecordingModel
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.ResourcedTrashRecordingModels
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.TrashRecordingsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

private const val LOGGER_TAG = "TRASHED_RECORDINGS_PROVIDER"

@RequiresApi(Build.VERSION_CODES.R)
class TrashRecordingsProviderImpl(
	private val context: Context,
	private val recordingsDao: RecordingsMetadataDao,
) : RecordingsProvider(context), TrashRecordingsProvider {

	override val trashedRecordingsFlow: Flow<ResourcedTrashRecordingModels>
		get() = callbackFlow {

			launch(Dispatchers.IO) {
				// for the first time we need the query the current
				val recordings = getTrashedVoiceRecordings()
				send(recordings)
			}

			val observer = object : ContentObserver(null) {
				override fun onChange(selfChange: Boolean) {
					super.onChange(selfChange)
					launch(Dispatchers.IO) {
						val recordings = getTrashedVoiceRecordings()
						send(recordings)
					}
				}
			}

			Log.d(LOGGER_TAG, "ADDED OBSERVER FOR TRASHED ITEMS")
			contentResolver.registerContentObserver(AUDIO_VOLUME_URI, true, observer)

			awaitClose {
				Log.d(LOGGER_TAG, "CANCELED OBSERVER FOR TRASH ITEMS")
				contentResolver.unregisterContentObserver(observer)
			}
		}

	override suspend fun getTrashedVoiceRecordings(): ResourcedTrashRecordingModels {
		// queries only the trash ones of this app
		val selection = "${MediaStore.Audio.AudioColumns.OWNER_PACKAGE_NAME} = ? "
		val selectionArgs = arrayOf(context.packageName)
		val sortColumns = arrayOf(MediaStore.Audio.AudioColumns.DATE_EXPIRES)

		val queryArgs = bundleOf(
			ContentResolver.QUERY_ARG_SQL_SELECTION to selection,
			ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS to selectionArgs,
			MediaStore.QUERY_ARG_MATCH_TRASHED to MediaStore.MATCH_ONLY,
			ContentResolver.QUERY_ARG_SORT_COLUMNS to sortColumns,
			ContentResolver.QUERY_ARG_SORT_DIRECTION to ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
		)

		return withContext(Dispatchers.IO) {
			try {
				val models = contentResolver
					.query(AUDIO_VOLUME_URI, trashRecordingsProjection, queryArgs, null)
					?.use(::readTrashedRecordingsFromCursor)
					?: emptyList()

				Resource.Success(models)
			} catch (e: SQLException) {
				Resource.Error(e, e.localizedMessage ?: "SQL EXCEPTION")
			} catch (e: SecurityException) {
				Resource.Error(e, e.localizedMessage ?: "SECURITY EXCEPTION")
			} catch (e: Exception) {
				e.printStackTrace()
				Resource.Error(e, e.message)
			}
		}
	}

	override suspend fun restoreRecordingsFromTrash(recordings: Collection<TrashRecordingModel>)
			: Resource<Unit, Exception> {
		return coroutineScope {
			// ensure that only this app files are restore.
			val recordingsWithOwnerShip = recordings.filter { it.owner == context.packageName }

			try {
				val restoreUris = async(Dispatchers.IO) {
					// restore uris from trash
					val uriToRestore = recordingsWithOwnerShip
						.map { model -> model.fileUri.toUri() }

					moveUrisToOrFromTrash(uriToRestore, fromTrash = true)
				}
				val createMetadata = async(Dispatchers.IO) {
					// create secondary metadata
					val entities = recordingsWithOwnerShip.map { RecordingsMetaDataEntity(it.id) }
					recordingsDao.addRecordingMetaDataBulk(entities)
				}
				awaitAll(restoreUris, createMetadata)

				val message = context.getString(R.string.restore_recordings_success)
				Resource.Success(Unit, message)
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				// on other exceptions
				e.printStackTrace()
				val errorMessage = context.getString(R.string.recording_restore_request_failed)
				Resource.Error(e, message = errorMessage)
			}
		}
	}

	override fun createTrashRecordings(recordings: Collection<RecordedVoiceModel>)
			: Flow<Resource<Collection<RecordedVoiceModel>, Exception>> {

		val recordingsWithOwnerShip = recordings.filter { it.owner == context.packageName }
		val recordingsWithoutOwnerShip = recordings.filterNot { it.owner == context.packageName }

		return flow<Resource<Collection<RecordedVoiceModel>, Exception>> {
			if (recordingsWithOwnerShip.isNotEmpty()) {
				try {
					// try to delete recordings with ownership
					val urisToDelete = recordingsWithOwnerShip.map { it.fileUri.toUri() }
					// perform action in dispatches IO
					coroutineScope {
						val moveTrashDeferred = async(Dispatchers.IO) {
							moveUrisToOrFromTrash(urisToDelete, fromTrash = false)
						}
						// clear metadata of all the selected ones
						val clearMetaDataDeferred = async(Dispatchers.IO) {
							val ids = recordings.map { it.id }
							// now remove secondary data from the table
							recordingsDao.deleteRecordingMetaDataFromIds(ids)
						}
						awaitAll(moveTrashDeferred, clearMetaDataDeferred)
					}
					val successMessage = context.getString(R.string.recording_trash_request_success)
					emit(Resource.Success(emptyList(), successMessage))
				} catch (e: SecurityException) {
					emit(Resource.Error(e, "Security Issues"))
				} catch (e: Exception) {
					e.printStackTrace()
					val message = context.getString(R.string.recording_trash_request_failed)
					emit(Resource.Error(e, message))
				}
			}
			// try to delete without ownership
			if (recordingsWithoutOwnerShip.isNotEmpty()) {
				emit(Resource.Error(error = CannotTrashFileDifferentOwnerException()))
			}
		}.flowOn(Dispatchers.IO)
	}


	override suspend fun permanentlyDeleteRecordingsInTrash(trashRecordings: Collection<TrashRecordingModel>): Resource<Unit, Exception> {

		val currentAppRecordings = trashRecordings.filter { it.owner == context.packageName }

		return withContext(Dispatchers.IO) {
			try {
				val uriToDeletePermanent =
					currentAppRecordings.map { model -> model.fileUri.toUri() }
				permanentDeleteUrisFromAudioMediaVolume(uriToDeletePermanent)

				val message = context.getString(R.string.recording_delete_request_success)
				Resource.Success(Unit, message)
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				e.printStackTrace()
				val errorMessage = context.getString(R.string.recording_delete_request_failed)
				Resource.Error(e, errorMessage)
			}
		}
	}
}