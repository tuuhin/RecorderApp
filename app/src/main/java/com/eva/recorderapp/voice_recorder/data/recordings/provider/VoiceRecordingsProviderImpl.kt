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
import com.eva.recorderapp.voice_recorder.domain.datastore.repository.RecorderFileSettingsRepo
import com.eva.recorderapp.voice_recorder.domain.recordings.exceptions.InvalidRecordingIdException
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.ResourcedVoiceRecordingModels
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.VoiceRecordingModels
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.VoiceRecordingsProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val LOGGER_TAG = "VOICE_RECORDINGS_PROVIDER"

class VoiceRecordingsProviderImpl(
	private val context: Context,
	private val fileSettingsRepo: RecorderFileSettingsRepo,
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
			contentResolver.registerContentObserver(AUDIO_VOLUME_URI, true, observer)

			awaitClose {
				Log.d(LOGGER_TAG, "CANCEL OBSERVER FOR RECORDINGS")
				scope.cancel()
				contentResolver.unregisterContentObserver(observer)
			}
		}

	override val voiceRecordingFlowAsResource: Flow<ResourcedVoiceRecordingModels>
		get() = flow {
			try {
				// emit loading
				emit(Resource.Loading)
				// emit the models
				emitAll(voiceRecordingsFlow.map { models -> Resource.Success(models) })
			} catch (e: Exception) {
				e.printStackTrace()
				emit(Resource.Error(e))
			}
		}

	override val voiceRecordingsOnlyThisApp: Flow<ResourcedVoiceRecordingModels>
		get() = flow {
			try {
				// emit loading
				emit(Resource.Loading)
				// emit the models
				emitAll(
					voiceRecordingsFlow.map { models ->
						val ownerShipTHisApp = models.filter { it.owner == context.packageName }
						Resource.Success(ownerShipTHisApp)
					},
				)
			} catch (e: Exception) {
				e.printStackTrace()
				emit(Resource.Error(e))
			}
		}

	override suspend fun getVoiceRecordings(): VoiceRecordingModels {
		val allowExternalRead = fileSettingsRepo.fileSettings.allowExternalRead
		val queryArgs = if (allowExternalRead && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			// they are of owner package and external recordings
			val selection = buildString {
				append(MediaStore.Audio.AudioColumns.IS_RECORDING)
				append(" = ? ")
			}
			val selectionArgs = arrayOf("1")
			bundleOf(
				ContentResolver.QUERY_ARG_SQL_SELECTION to selection,
				ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS to selectionArgs,
			)
		} else {
			// only owner package
			val selection = buildString {
				append(MediaStore.Audio.AudioColumns.OWNER_PACKAGE_NAME)
				append(" = ? ")
			}
			val selectionArgs = arrayOf(context.packageName)
			// items only of this package
			bundleOf(
				ContentResolver.QUERY_ARG_SQL_SELECTION to selection,
				ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS to selectionArgs,
			)
		}

		val sortColumns = arrayOf(MediaStore.Audio.AudioColumns.DATE_ADDED)
		val otherExtras = bundleOf(
			ContentResolver.QUERY_ARG_SORT_COLUMNS to sortColumns,
			ContentResolver.QUERY_ARG_SORT_DIRECTION to ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
		)

		val selectionBundle = queryArgs.apply { putAll(otherExtras) }

		return withContext(Dispatchers.IO) {
			contentResolver.query(AUDIO_VOLUME_URI, recordingsProjection, selectionBundle, null)
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
		val recordingUri = ContentUris.withAppendedId(AUDIO_VOLUME_URI, recordingId)
		return try {
			val models = withContext(Dispatchers.IO) {
				contentResolver.query(recordingUri, recordingsProjection, null, null)
					?.use(::readNormalRecordingsFromCursor)
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
				return@withContext Resource.Success(deleteRow == 1)
			} catch (e: SecurityException) {
				Resource.Error(e, "SECURITY EXCEPTION")
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				e.printStackTrace()
				Resource.Error(e)
			}
		}
	}

	override suspend fun deleteFileFromId(id: Long): Resource<Boolean, Exception> {
		val selection = "${MediaStore.Audio.AudioColumns._ID} = ? "
		val selectionArgs = arrayOf("$id")
		val bundle = bundleOf(
			ContentResolver.QUERY_ARG_SQL_SELECTION to selection,
			ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS to selectionArgs
		)

		return withContext(Dispatchers.IO) {
			try {
				val deleteRow = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
					contentResolver.delete(AUDIO_VOLUME_URI, bundle)
				else contentResolver.delete(AUDIO_VOLUME_URI, selection, selectionArgs)

				if (deleteRow == 1) return@withContext Resource.Success(true)
				return@withContext Resource.Success(false)
			} catch (e: SecurityException) {
				Resource.Error(e, "SECURITY EXCEPTION")
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				e.printStackTrace()
				Resource.Error(e)
			}
		}
	}

	override suspend fun permanentlyDeleteRecordedVoices(recordings: Collection<RecordedVoiceModel>)
			: Resource<Unit, Exception> {

		val filesFromThisApp = recordings.filter { it.owner == context.packageName }

		return withContext(Dispatchers.IO) {
			try {
				val urisToDelete = filesFromThisApp.map { model -> model.fileUri.toUri() }
				permanentDeleteUrisFromAudioMediaVolume(urisToDelete)
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

	override fun renameRecording(recording: RecordedVoiceModel, newName: String)
			: Flow<Resource<Boolean, Exception>> {
		return flow {
			emit(Resource.Loading)

			try {
				val uri = recording.fileUri.toUri()

				val contentValues = ContentValues().apply {
					put(MediaStore.Audio.AudioColumns.DISPLAY_NAME, newName)
				}

				val rowsModified = withContext(Dispatchers.IO) {
					contentResolver.update(uri, contentValues, null, null)
				}

				emit(
					Resource.Success(
						data = rowsModified >= 1,
						message = context.getString(R.string.rename_recording_success)
					)
				)
			} catch (e: SecurityException) {
				emit(Resource.Error(e, message = "Access not found"))
			} catch (e: SQLException) {
				emit(Resource.Error(e, "SQL EXCEPTION"))
			} catch (e: Exception) {
				e.printStackTrace()
				emit(Resource.Error(e))
			}
		}
	}
}