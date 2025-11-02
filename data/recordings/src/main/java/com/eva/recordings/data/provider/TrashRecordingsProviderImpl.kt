package com.eva.recordings.data.provider

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
import com.eva.database.dao.RecordingsMetadataDao
import com.eva.database.entity.RecordingsMetaDataEntity
import com.eva.recordings.R
import com.eva.recordings.data.wrapper.RecordingsConstants
import com.eva.recordings.data.wrapper.RecordingsContentResolverWrapper
import com.eva.recordings.domain.models.RecordedVoiceModel
import com.eva.recordings.domain.models.TrashRecordingModel
import com.eva.recordings.domain.provider.ResourcedTrashRecordingModels
import com.eva.recordings.domain.provider.TrashRecordingsProvider
import com.eva.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

private const val LOGGER_TAG = "TRASHED_RECORDINGS_PROVIDER"

@RequiresApi(Build.VERSION_CODES.R)
internal class TrashRecordingsProviderImpl(
	private val context: Context,
	private val recordingsDao: RecordingsMetadataDao,
) : RecordingsContentResolverWrapper(context), TrashRecordingsProvider {

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
			contentResolver.registerContentObserver(
				RecordingsConstants.AUDIO_VOLUME_URI,
				true,
				observer
			)

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
				val models = contentResolver.query(
					RecordingsConstants.AUDIO_VOLUME_URI,
					trashRecordingsProjection,
					queryArgs,
					null
				)
					?.use(::readTrashedRecordingsFromCursor)
					?: emptyList()

				Resource.Success(models)
			} catch (e: SQLException) {
				Resource.Error(e, e.localizedMessage ?: "SQL EXCEPTION")
			} catch (e: SecurityException) {
				Resource.Error(e, e.localizedMessage ?: "SECURITY EXCEPTION")
			} catch (e: Exception) {
				if (e is CancellationException) throw e
				e.printStackTrace()
				Resource.Error(e, e.message)
			}
		}
	}

	override suspend fun restoreRecordingsFromTrash(recordings: Collection<TrashRecordingModel>)
			: Resource<Unit, Exception> {
		// ensure that only this app files are restore.
		val recordingsWithOwnerShip = recordings.filter { it.owner == context.packageName }

		return try {
			// restore uris from trash
			val uriToRestore = recordingsWithOwnerShip
				.map { model -> model.fileUri.toUri() }
				.toSet()

			moveToTrashOrRestoreFromTrash(uriToRestore, fromTrash = true)
			// then create secondary metadata : kept it synchronous as after delete only this should be performed
			val entities = recordingsWithOwnerShip.map { RecordingsMetaDataEntity(it.id) }
			recordingsDao.addRecordingMetaDataBulk(entities)

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

	override fun createTrashRecordings(recordings: Collection<RecordedVoiceModel>)
			: Flow<Resource<Collection<RecordedVoiceModel>, Exception>> {

		val ownedRecordings = recordings.filter { it.owner == context.packageName }.toSet()
		val notOwnedRecordings = recordings.filter { it.owner != context.packageName }.toSet()

		return flow {
			// handle the ownership recordings
			if (ownedRecordings.isNotEmpty()) {
				try {
					val urisToDelete = ownedRecordings.map { it.fileUri.toUri() }.toSet()
					// move items to trash
					moveToTrashOrRestoreFromTrash(urisToDelete, fromTrash = false)
					// emit a success with empty list
					val result: Resource.Success<List<RecordedVoiceModel>, Exception> =
						Resource.Success(
							emptyList(),
							context.getString(R.string.recording_trash_request_success)
						)
					emit(result)

					// now delete the associated entries
					val ids = recordings.map { it.id }
					// now remove secondary data from the table
					recordingsDao.deleteRecordingMetaDataFromIds(ids)
				} catch (e: Exception) {
					e.printStackTrace()
					val message = context.getString(R.string.recording_trash_request_failed)
					emit(Resource.Error(e, message))
				}
			}
			if (notOwnedRecordings.isNotEmpty()) {
				try {
					// this is sure to throw a security exception
					val recordingsURIs = notOwnedRecordings.map { it.fileUri.toUri() }.toSet()
					moveToTrashOrRestoreFromTrash(recordingsURIs, fromTrash = false)
				} catch (e: SecurityException) {
					Log.e(LOGGER_TAG, "Trying to trash non owner recordings", e)
					// now in send the exception that it has failed to remove
					// this need to be handled via recoverable security exception
					emit(Resource.Error(e, data = notOwnedRecordings))
				}
			}
		}
	}


	override fun permanentlyDeleteRecordingsInTrash(trashRecordings: List<TrashRecordingModel>)
			: Flow<Resource<List<TrashRecordingModel>, Exception>> {

		val ownedRecordings = trashRecordings.filter { it.owner == context.packageName }.toSet()
		val notOwnedRecordings = trashRecordings.filter { it.owner != context.packageName }.toSet()

		return flow {
			if (ownedRecordings.isNotEmpty()) {
				try {
					val urisToDelete = ownedRecordings.map { it.fileUri.toUri() }.toSet()
					// move items to trash
					permanentlyDeleteURIFromScopedStorage(urisToDelete)
					// then clear the associated
					val result: Resource.Success<List<TrashRecordingModel>, Exception> =
						Resource.Success(
							emptyList(),
							context.getString(R.string.recording_trash_request_success)
						)
					emit(result)
				} catch (e: Exception) {
					e.printStackTrace()
					val message = context.getString(R.string.recording_trash_request_failed)
					emit(Resource.Error(e, message))
				}
			}
			if (notOwnedRecordings.isNotEmpty()) {
				try {
					val urisToDelete = notOwnedRecordings.map { it.fileUri.toUri() }
						.toSet()
					// this is sure to expose it throw a security exception
					permanentlyDeleteURIFromScopedStorage(urisToDelete)
				} catch (e: SecurityException) {
					Log.e(LOGGER_TAG, "Trying permanently delete non owner uris", e)
					// now in send the exception that it has failed to remove
					// this need to be handled via recoverable security exception
					emit(Resource.Error(e, data = notOwnedRecordings.toList()))
				}
			}
		}.flowOn(Dispatchers.IO)
	}

	override suspend fun permanentlyDeleteRecordings(trashRecordings: List<TrashRecordingModel>): Resource<Unit, Exception> {
		val ownedRecordings = trashRecordings.filter { it.owner == context.packageName }
		return withContext(Dispatchers.IO) {
			try {
				val urisToDelete = ownedRecordings.map { it.fileUri.toUri() }.toSet()
				// move items to trash
				permanentlyDeleteURIFromScopedStorage(urisToDelete)
				// then clear the associated
				Resource.Success(
					Unit,
					context.getString(R.string.recording_trash_request_success)
				)
			} catch (e: Exception) {
				e.printStackTrace()
				val message = context.getString(R.string.recording_trash_request_failed)
				Resource.Error(e, message)
			}
		}
	}
}