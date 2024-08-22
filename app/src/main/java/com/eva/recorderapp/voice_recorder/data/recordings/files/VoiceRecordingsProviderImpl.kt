package com.eva.recorderapp.voice_recorder.data.recordings.files

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
import androidx.core.net.toUri
import com.eva.recorderapp.R
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.ResourcedVoiceRecordingModels
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.VoiceRecordingsProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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
				override fun onChange(selfChange: Boolean) {
					super.onChange(selfChange)

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
				val models = contentResolver.query(volumeUri, recordingsProjection, queryArgs, null)
					?.use { cursor -> readNormalRecordingsFromCursor(cursor = cursor) }
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

	override suspend fun permanentlyDeleteRecordedVoices(recordings: Collection<RecordedVoiceModel>): Resource<Unit, Exception> {
		return withContext(Dispatchers.IO) {
			supervisorScope {
				try {
					val deleteRequests = recordings.map { model ->
						val uri = model.fileUri.toUri()
						async { permanentDeleteFromStorage(uri) }
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

	override suspend fun renameRecording(recording: RecordedVoiceModel, newName: String)
			: Flow<Resource<Boolean, Exception>> {
		return flow {
			emit(Resource.Loading)
			try {
				val uri = recording.fileUri.toUri()
				val contentValues = ContentValues().apply {
					put(MediaStore.Audio.AudioColumns.DISPLAY_NAME, newName)
					put(MediaStore.Audio.AudioColumns.DATE_MODIFIED, System.currentTimeMillis())
				}
				val rowsModified = contentResolver.update(uri, contentValues, null, null)
				emit(Resource.Success(rowsModified == 1))
			} catch (e: SQLException) {
				emit(Resource.Error(e, "SQL EXCEPTION"))
			} catch (e: Exception) {
				e.printStackTrace()
				emit(Resource.Error(e))
			}
		}.flowOn(Dispatchers.IO)
	}

}