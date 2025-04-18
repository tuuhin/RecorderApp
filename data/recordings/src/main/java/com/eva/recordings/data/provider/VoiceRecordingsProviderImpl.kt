package com.eva.recordings.data.provider

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.database.SQLException
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import com.eva.datastore.domain.repository.RecorderFileSettingsRepo
import com.eva.recordings.R
import com.eva.recordings.data.wrapper.RecordingsConstants
import com.eva.recordings.data.wrapper.RecordingsContentResolverWrapper
import com.eva.recordings.domain.exceptions.InvalidRecordingIdException
import com.eva.recordings.domain.exceptions.NoRecordingsModifiedOrDeletedException
import com.eva.recordings.domain.models.RecordedVoiceModel
import com.eva.recordings.domain.provider.ResourcedVoiceRecordingModels
import com.eva.recordings.domain.provider.VoiceRecordingModels
import com.eva.recordings.domain.provider.VoiceRecordingsProvider
import com.eva.utils.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val LOGGER_TAG = "VOICE_RECORDINGS_PROVIDER"

internal class VoiceRecordingsProviderImpl(
	private val context: Context,
	private val fileSettingsRepo: RecorderFileSettingsRepo,
) : RecordingsContentResolverWrapper(context), VoiceRecordingsProvider {

	private fun recordingFlowWithExternalReadEnabled(allowExternalRead: Boolean)
			: Flow<VoiceRecordingModels> {
		return callbackFlow {

			launch(Dispatchers.IO) {
				val recordings = getVoiceRecordings(allowExternalRead)
				send(recordings)
			}

			val observer = object : ContentObserver(null) {
				override fun onChange(selfChange: Boolean) {
					super.onChange(selfChange)
					// observer has found some changes launch a new coroutine to update data
					launch(Dispatchers.IO) {
						val recordings = getVoiceRecordings(allowExternalRead)
						send(recordings)
					}
				}
			}

			Log.d(LOGGER_TAG, "ADDED OBSERVER FOR VOICE RECORDINGS")
			contentResolver.registerContentObserver(RecordingsConstants.AUDIO_VOLUME_URI, true, observer)

			awaitClose {
				// the launch will get automatically cancelled when closed
				Log.d(LOGGER_TAG, "CANCEL OBSERVER FOR RECORDINGS")
				contentResolver.unregisterContentObserver(observer)
			}
		}
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	override val voiceRecordingsFlow: Flow<VoiceRecordingModels>
		get() = fileSettingsRepo.fileSettingsFlow
			.map { settings -> settings.allowExternalRead }
			.distinctUntilChanged()
			.flatMapLatest(::recordingFlowWithExternalReadEnabled)
			.catch { err -> err.printStackTrace() }


	override val voiceRecordingsOnlyThisApp: Flow<ResourcedVoiceRecordingModels>
		get() = flow {
			try {
				// emit loading
				emit(Resource.Loading)
				// emit the models
				val recordings = recordingFlowWithExternalReadEnabled(false).map {
					Resource.Success<VoiceRecordingModels, Exception>(data = it)
				}
				// emit the recordings with the correct owner name
				emitAll(recordings)
			} catch (e: Exception) {
				e.printStackTrace()
				emit(Resource.Error(e))
			}
		}

	override suspend fun getVoiceRecordings(queryOthers: Boolean): VoiceRecordingModels {
		val queryArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && queryOthers) {
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
			contentResolver.query(
				RecordingsConstants.AUDIO_VOLUME_URI,
				recordingsProjection,
				selectionBundle,
				null
			)
				?.use { cursor -> readNormalRecordingsFromCursor(cursor) }
				?: emptyList()
		}
	}


	override suspend fun getVoiceRecordingAsResourceFromId(recordingId: Long): Resource<RecordedVoiceModel, Exception> {
		val recordingUri = ContentUris.withAppendedId(RecordingsConstants.AUDIO_VOLUME_URI, recordingId)
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


	override suspend fun deleteFileFromUri(uri: String): Resource<Unit, Exception> {
		return withContext(Dispatchers.IO) {
			try {
				val deleteRow = contentResolver.delete(uri.toUri(), null, null)
				return@withContext if (deleteRow == 1)
					Resource.Success<Unit, Exception>(
						data = Unit,
						message = context.getString(R.string.rename_recording_success)
					)
				else Resource.Error(NoRecordingsModifiedOrDeletedException())

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

	override suspend fun deleteFileFromId(id: Long): Resource<Unit, Exception> {
		val selection = "${MediaStore.Audio.AudioColumns._ID} = ? "
		val selectionArgs = arrayOf("$id")
		val bundle = bundleOf(
			ContentResolver.QUERY_ARG_SQL_SELECTION to selection,
			ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS to selectionArgs
		)

		return withContext(Dispatchers.IO) {
			try {
				val deleteRow = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
					contentResolver.delete(RecordingsConstants.AUDIO_VOLUME_URI, bundle)
				else contentResolver.delete(RecordingsConstants.AUDIO_VOLUME_URI, selection, selectionArgs)

				return@withContext if (deleteRow == 1)
					Resource.Success<Unit, Exception>(
						data = Unit,
						message = context.getString(R.string.rename_recording_success)
					)
				else Resource.Error(NoRecordingsModifiedOrDeletedException())

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
			: Flow<Resource<Unit, Exception>> {
		return flow {
			try {
				val uri = recording.fileUri.toUri()

				val contentValues = ContentValues().apply {
					put(MediaStore.Audio.AudioColumns.DISPLAY_NAME, newName)
				}
				emit(Resource.Loading)

				val transaction = withContext(Dispatchers.IO) {
					contentResolver.update(uri, contentValues, null, null)
				}
				val result = if (transaction == 1)
					Resource.Success<Unit, Exception>(
						data = Unit,
						message = context.getString(R.string.rename_recording_success)
					)
				else Resource.Error(NoRecordingsModifiedOrDeletedException())

				emit(result)

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