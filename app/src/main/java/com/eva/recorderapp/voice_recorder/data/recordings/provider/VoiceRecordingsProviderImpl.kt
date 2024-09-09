package com.eva.recorderapp.voice_recorder.data.recordings.provider

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.database.SQLException
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import com.eva.recorderapp.R
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.recordings.exceptions.InvalidRecordingIdException
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.ResourcedVoiceRecordingModels
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.VoiceRecordingModels
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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

private const val LOGGER_TAG = "VOICE_RECORDINGS_PROVIDER"

class VoiceRecordingsProviderImpl(
	private val context: Context,
) : RecordingsProvider(context), VoiceRecordingsProvider {

	override val voiceRecordingsFlow: Flow<VoiceRecordingModels>
		get() = callbackFlow {

			val scope = CoroutineScope(Dispatchers.IO)

			scope.launch {
				val recordings = getVoiceRecordings()
				send(recordings)
			}

			val observer = object : ContentObserver(null) {
				override fun onChange(selfChange: Boolean) {
					super.onChange(selfChange)

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

	override val voiceRecordingFlowAsResource: Flow<ResourcedVoiceRecordingModels>
		get() = flow {
			try {
				val recordings = voiceRecordingsFlow.map { models ->
					Resource.Success<VoiceRecordingModels, Exception>(models)
				}
				emitAll(recordings)
			} catch (e: Exception) {
				e.printStackTrace()
				emit(Resource.Error(e))
			}
		}

	override suspend fun getVoiceRecordings(): VoiceRecordingModels {
		val otherRecordings = false

		val queryArgs = if (otherRecordings && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			val selection = "${MediaStore.Audio.AudioColumns.IS_RECORDING} = ?"
			val selectionArgs = arrayOf("1")
			val sortColumns = arrayOf(MediaStore.Audio.AudioColumns.DATE_ADDED)
			// items of type recordings
			bundleOf(
				ContentResolver.QUERY_ARG_SQL_SELECTION to selection,
				ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS to selectionArgs,
				ContentResolver.QUERY_ARG_SORT_COLUMNS to sortColumns,
				ContentResolver.QUERY_ARG_SORT_DIRECTION to
						ContentResolver.QUERY_SORT_DIRECTION_DESCENDING,
			)
		} else {
			val selection = "${MediaStore.Audio.AudioColumns.OWNER_PACKAGE_NAME} = ?"
			val selectionArgs = arrayOf(context.packageName)
			val sortColumns = arrayOf(MediaStore.Audio.AudioColumns.DATE_ADDED)
			// items only of this package
			bundleOf(
				ContentResolver.QUERY_ARG_SQL_SELECTION to selection,
				ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS to selectionArgs,
				ContentResolver.QUERY_ARG_SORT_COLUMNS to sortColumns,
				ContentResolver.QUERY_ARG_SORT_DIRECTION to
						ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
			)
		}

		return withContext(Dispatchers.IO) {
			contentResolver.query(volumeUri, recordingsProjection, queryArgs, null)
				?.use { cursor -> readNormalRecordingsFromCursor(cursor) }
				?: emptyList()
		}
	}

	override suspend fun getVoiceRecordingsAsResource(): ResourcedVoiceRecordingModels {
		return withContext(Dispatchers.IO) {
			try {
				val models = getVoiceRecordings()
				Resource.Success(models)
			} catch (e: SQLException) {
				Resource.Error(e, "SQL EXCEPTION")
			} catch (e: Exception) {
				e.printStackTrace()
				Resource.Error(e, e.message)
			}
		}
	}

	override suspend fun getVoiceRecordingAsResourceFromId(recordingId: Long): Resource<RecordedVoiceModel, Exception> {
		val recordingUri = ContentUris.withAppendedId(volumeUri, recordingId)
		return try {
			val models = withContext(Dispatchers.IO) {
				contentResolver.query(recordingUri, recordingsProjection, null, null)
					?.use { cursor -> readNormalRecordingsFromCursor(cursor) }
					?: emptyList()
			}
			models.firstOrNull()?.let { result -> Resource.Success(data = result) }
				?: Resource.Error(InvalidRecordingIdException())
		} catch (e: Exception) {
			Resource.Error(e)
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
		val bundle = bundleOf(
			ContentResolver.QUERY_ARG_SQL_SELECTION to selection,
			ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS to selectionArgs
		)

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

	override suspend fun permanentlyDeleteRecordedVoices(
		recordings: Collection<RecordedVoiceModel>,
	): Resource<Unit, Exception> {
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
					val errorMessage = context.getString(R.string.recording_delete_request_failed)
					Resource.Error(e, errorMessage)
				}
			}
		}
	}

	override fun renameRecording(recording: RecordedVoiceModel, newName: String)
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

				val res = Resource.Success<Boolean, Exception>(
					data = rowsModified == 1,
					message = context.getString(R.string.rename_recording_success)
				)
				emit(res)
			} catch (e: SQLException) {
				emit(Resource.Error(e, "SQL EXCEPTION"))
			} catch (e: Exception) {
				e.printStackTrace()
				emit(Resource.Error(e))
			}
		}.flowOn(Dispatchers.IO)
	}
}