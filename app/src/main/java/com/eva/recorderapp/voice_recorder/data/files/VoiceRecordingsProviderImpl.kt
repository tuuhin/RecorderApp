package com.eva.recorderapp.voice_recorder.data.files

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.database.SQLException
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.core.database.getIntOrNull
import androidx.core.net.toUri
import com.eva.recorderapp.R
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.files.ResourcedVoiceRecordingModels
import com.eva.recorderapp.voice_recorder.domain.files.VoiceRecordingsProvider
import com.eva.recorderapp.voice_recorder.domain.models.RecordedVoiceModel
import kotlinx.coroutines.CancellationException
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

private const val LOGGER_TAG = "VOICE_RECORDINGS_PROVIDER"

class VoiceRecordingsProviderImpl(
	private val context: Context
) : RecordingsUtils(context), VoiceRecordingsProvider {

	override val voiceRecordingsFlow: Flow<ResourcedVoiceRecordingModels>
		get() = callbackFlow {

			val scope = CoroutineScope(Dispatchers.IO)

			trySend(Resource.Loading)

			scope.launch {
				val recordings = getVoiceRecordings()
				send(recordings)
			}

			val observer = object : ContentObserver(null) {
				override fun onChange(selfChange: Boolean, uri: Uri?, flags: Int) {
					super.onChange(selfChange, uri, flags)

					Log.d(LOGGER_TAG, "RECORDINGS CONTENT CHANGED")
					// if the content updated then resend the values
					scope.launch {
						val recordings = getVoiceRecordings()
						send(recordings)
					}
				}
			}

			Log.d(LOGGER_TAG, "ADDED OBSERVER FOR VOICE RECORDINGS")
			contentResolver.registerContentObserver(volumeUri, true, observer)

			awaitClose {
				Log.d(LOGGER_TAG, "CANCEL OBSERVER FOR RECORDINGS")
				scope.cancel()
				contentResolver.unregisterContentObserver(observer)
			}
		}

	override suspend fun getVoiceRecordings(): ResourcedVoiceRecordingModels {
		val selection = "${MediaStore.Audio.AudioColumns.OWNER_PACKAGE_NAME} = ?"
		val selectionArgs = arrayOf(context.packageName)

		val queryArgs = Bundle().apply {
			//selection
			putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
			putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
			//sorting
			putStringArray(
				ContentResolver.QUERY_ARG_SORT_COLUMNS,
				arrayOf(MediaStore.Audio.AudioColumns.DATE_ADDED)
			)
			putInt(
				ContentResolver.QUERY_ARG_SORT_DIRECTION,
				ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
			)
		}

		return withContext(Dispatchers.IO) {
			try {
				val models = contentResolver.query(volumeUri, baseProjection, queryArgs, null)
					?.use { cursor -> recordingsFromCursor(cursor = cursor, volumeUri = volumeUri) }
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


	override suspend fun deleteFileFromUri(uri: Uri): Resource<Boolean, Exception> {
		return withContext(Dispatchers.IO) {
			try {
				val deleteRow = contentResolver.delete(uri, null, null)
				if (deleteRow == 1) return@withContext Resource.Success(true)
				return@withContext Resource.Success(false)
			} catch (e: SecurityException) {
				Resource.Error(e, "SECURITY EXCEPTION")
			} catch (e: Exception) {
				e.printStackTrace()
				Resource.Error(e)
			}
		}
	}

	override suspend fun deleteFileFromId(id: Long): Resource<Boolean, Exception> {
		val selection = "${MediaStore.Audio.AudioColumns._ID}=?"
		val selectionArgs = arrayOf("$id")
		val bundle = Bundle().apply {
			//selection
			putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
			putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
		}

		return withContext(Dispatchers.IO) {
			try {

				val deleteRow = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
					contentResolver.delete(volumeUri, bundle)
				else contentResolver.delete(volumeUri, selection, selectionArgs)

				if (deleteRow == 1) return@withContext Resource.Success(true)
				return@withContext Resource.Success(false)
			} catch (e: SecurityException) {
				Resource.Error(e, "SECURITY EXCEPTION")
			} catch (e: Exception) {
				e.printStackTrace()
				Resource.Error(e)
			}
		}
	}

	override suspend fun createTrashRecordings(models: Collection<RecordedVoiceModel>): Resource<Unit, Exception> {
		return withContext(Dispatchers.IO) {
			try {
				supervisorScope {
					val trashRequests = models.map { model ->
						val uri = model.fileUri.toUri()
						async { moveUriToTrash(uri) }
					}
					trashRequests.awaitAll()
					Resource.Success(
						data = Unit,
						message = context.getString(R.string.recording_trash_request_success)
					)
				}
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

	private suspend fun moveUriToTrash(uri: Uri): Boolean {
		return withContext(Dispatchers.IO) {

			val isTrashed = checkIfUriAlreadyTrashed(uri)
			if (isTrashed) {
				Log.d(LOGGER_TAG, "URI IS ALREADY IN BIN")
				return@withContext false
			}

			val updatedMetaData = ContentValues().apply {
				put(MediaStore.Audio.AudioColumns.IS_TRASHED, 1)
				put(MediaStore.Audio.AudioColumns.DATE_MODIFIED, System.currentTimeMillis())
			}
			contentResolver.update(uri, updatedMetaData, null, null)
			Log.d(LOGGER_TAG, "URI MOVED TO TRASH")
			return@withContext true
			// single row affected i.e, the updated file
		}
	}

	private suspend fun checkIfUriAlreadyTrashed(uri: Uri): Boolean {
		val projection = arrayOf(MediaStore.Audio.AudioColumns.IS_TRASHED)
		return contentResolver.query(uri, projection, Bundle(), null)?.use { cursor ->
			val column = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.IS_PENDING)

			if (!cursor.moveToFirst()) return false

			val isTrashed = cursor.getIntOrNull(column)
			// its already updated so no need to delete
			return isTrashed == 1

		} ?: false
	}

}