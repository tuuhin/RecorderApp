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
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.domain.recordings.models.TrashRecordingModel
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.ResourcedTrashRecordingModels
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.TrashRecordingsProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

private const val LOGGER_TAG = "TRASHED_RECORDINGS_PROVIDER"

@RequiresApi(Build.VERSION_CODES.R)
class TrashRecordingsProviderImpl(
	private val context: Context
) : RecordingsProvider(context), TrashRecordingsProvider {

	override val trashedRecordingsFlow: Flow<ResourcedTrashRecordingModels>
		get() = callbackFlow {

			val scope = CoroutineScope(Dispatchers.IO)
			trySend(Resource.Loading)

			scope.launch {
				// for the first time we need the query the current
				val recordings = getTrashedVoiceRecordings()
				send(recordings)
			}

			val observer = object : ContentObserver(null) {
				override fun onChange(selfChange: Boolean) {
					super.onChange(selfChange)
					scope.launch {
						val recordings = getTrashedVoiceRecordings()
						send(recordings)
					}
				}
			}

			Log.d(LOGGER_TAG, "ADDED OBSERVER FOR TRASHED ITEMS")
			contentResolver.registerContentObserver(volumeUri, true, observer)

			awaitClose {
				Log.d(LOGGER_TAG, "CANCELED OBSERVER FOR TRASH ITEMS")
				scope.cancel()
				contentResolver.unregisterContentObserver(observer)
			}
		}

	override suspend fun getTrashedVoiceRecordings(): ResourcedTrashRecordingModels {

		val selection = "${MediaStore.Audio.AudioColumns.OWNER_PACKAGE_NAME} = ? "
		val selectionArgs = arrayOf(context.packageName)
		val sortColumns = arrayOf(MediaStore.Audio.AudioColumns.DATE_MODIFIED)

		return withContext(Dispatchers.IO) {
			try {
				val queryArgs = bundleOf(
					ContentResolver.QUERY_ARG_SQL_SELECTION to selection,
					ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS to selectionArgs,
					MediaStore.QUERY_ARG_MATCH_TRASHED to MediaStore.MATCH_ONLY,
					ContentResolver.QUERY_ARG_SORT_COLUMNS to sortColumns,
					ContentResolver.QUERY_ARG_SORT_DIRECTION to ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
				)

				val models = contentResolver
					.query(volumeUri, trashRecordingsProjection, queryArgs, null)
					?.use(::readTrashedRecordingsFromCursor)
					?: emptyList()

				Resource.Success(models)
			} catch (e: SQLException) {
				Resource.Error(e, "SQL EXCEPTION")
			} catch (e: Exception) {
				e.printStackTrace()
				Resource.Error(e, e.message)
			}
		}
	}

	override suspend fun restoreRecordingsFromTrash(
		recordings: Collection<TrashRecordingModel>
	): Resource<Unit, Exception> {
		return withContext(Dispatchers.IO) {
			supervisorScope {
				try {
					val trashRequests = recordings.map { model ->
						val uri = model.fileUri.toUri()
						async { removeUriFromTrash(uri) }
					}
					trashRequests.awaitAll()
					Resource.Success(
						data = Unit,
						message = context.getString(R.string.restore_recordings_success)
					)
				} catch (e: CancellationException) {
					throw e
				} catch (e: Exception) {
					e.printStackTrace()
					val errorMessage = context.getString(R.string.recording_restore_request_failed)
					Resource.Error(e, errorMessage)
				}
			}
		}
	}

	override suspend fun createTrashRecordings(
		recordings: Collection<RecordedVoiceModel>
	): Resource<Unit, Exception> {
		return withContext(Dispatchers.IO) {
			supervisorScope {
				try {
					val trashRequests = recordings.map { model ->
						val uri = model.fileUri.toUri()
						async(Dispatchers.IO) { moveUriToTrash(uri) }
					}
					trashRequests.awaitAll()
					Resource.Success(
						data = Unit,
						message = context.getString(R.string.recording_trash_request_success)
					)
				} catch (e: CancellationException) {
					throw e
				} catch (e: Exception) {
					e.printStackTrace()
					Resource.Error(
						error = e,
						message = context.getString(R.string.recording_trash_request_failed)
					)
				}
			}
		}
	}

	override suspend fun permanentlyDeleteRecordedVoicesInTrash(
		trashRecordings: Collection<TrashRecordingModel>
	): Resource<Unit, Exception> {
		return withContext(Dispatchers.IO) {
			supervisorScope {
				try {
					val deleteRequests = trashRecordings.map { model ->
						val contentUri = model.fileUri.toUri()
						async { permanentDeleteFromStorage(contentUri) }
					}
					deleteRequests.awaitAll()
					Resource.Success(
						data = Unit,
						message = context.getString(R.string.recording_delete_request_success)
					)
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
}