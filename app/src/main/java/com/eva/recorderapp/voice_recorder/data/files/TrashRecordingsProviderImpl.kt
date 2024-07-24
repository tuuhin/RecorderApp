package com.eva.recorderapp.voice_recorder.data.files

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.database.SQLException
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.eva.recorderapp.R
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.files.ResourcedTrashRecordingModels
import com.eva.recorderapp.voice_recorder.domain.files.TrashRecordingsProvider
import com.eva.recorderapp.voice_recorder.domain.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.domain.models.TrashRecordingModel
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

private const val LOGGER_TAG = "TRASHED_RECORINGS_PROVIDER"

@RequiresApi(Build.VERSION_CODES.R)
class TrashRecordingsProviderImpl(
	private val context: Context
) : RecordingsUtils(context), TrashRecordingsProvider {

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

					Log.d(LOGGER_TAG, "CONTENT CHANGED")
					// if the content updated then resend the values
					scope.launch {
						val recordings = getTrashedVoiceRecordings()
						send(recordings)
					}
				}
			}

			Log.d(LOGGER_TAG, "ADDED OBSERVER FOR TRAHSED ITEMS")
			contentResolver.registerContentObserver(volumeUri, true, observer)

			awaitClose {
				Log.d(LOGGER_TAG, "CANCELED OBSERVER FOR TRASH ITEMS")
				scope.cancel()
				contentResolver.unregisterContentObserver(observer)
			}
		}

	override suspend fun getTrashedVoiceRecordings(): ResourcedTrashRecordingModels {

		return withContext(Dispatchers.IO) {
			try {

				val selection = "${MediaStore.Audio.AudioColumns.OWNER_PACKAGE_NAME} = ? "
				val selectionArgs = arrayOf(context.packageName)

				val queryArgs = Bundle().apply {
					//selection only this app file
					putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
					putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
					// show only trashed items
					putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
					//sorting
					putStringArray(
						ContentResolver.QUERY_ARG_SORT_COLUMNS,
						arrayOf(MediaStore.Audio.AudioColumns.DATE_MODIFIED)
					)
					putInt(
						ContentResolver.QUERY_ARG_SORT_DIRECTION,
						ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
					)
				}
				val models = contentResolver
					.query(volumeUri, trashRecoringsProjection, queryArgs, null)
					?.use { cursor -> readTrashedRecordingsFromCursor(cursor) }
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

	override suspend fun restoreRecordingsFromTrash(trashRecordings: Collection<TrashRecordingModel>): Resource<Unit, Exception> {
		return withContext(Dispatchers.IO) {
			supervisorScope {
				try {
					val trashRequests = trashRecordings.map { model ->
						val uri = model.fileUri.toUri()
						async { removeUriFromTrash(uri) }
					}
					trashRequests.awaitAll()
					Resource.Success(
						data = Unit,
						message = context.getString(R.string.recording_restore_request)
					)
				} catch (e: CancellationException) {
					throw e
				} catch (e: Exception) {
					e.printStackTrace()

					val errorMessage = context.getString(R.string.recording_restore_request_falied)
					Resource.Error(e, errorMessage)
				}
			}
		}
	}

	override suspend fun createTrashRecordings(recordings: Collection<RecordedVoiceModel>): Resource<Unit, Exception> {
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
						message = context.getString(R.string.recording_trash_request_falied)
					)
				}
			}
		}
	}

	override suspend fun permanentlyDeleteRecordedVoicesInTrash(trashRecordings: Collection<TrashRecordingModel>): Resource<Unit, Exception> {
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
					val errorMessage = context.getString(R.string.recording_delete_request_falied)
					Resource.Error(e, errorMessage)
				}
			}
		}
	}
}