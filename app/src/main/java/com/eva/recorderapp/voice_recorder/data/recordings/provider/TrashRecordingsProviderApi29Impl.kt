package com.eva.recorderapp.voice_recorder.data.recordings.provider

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.eva.recorderapp.R
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.data.database.dao.RecordingsMetadataDao
import com.eva.recorderapp.voice_recorder.data.database.dao.TrashFileDao
import com.eva.recorderapp.voice_recorder.data.database.entity.RecordingsMetaDataEntity
import com.eva.recorderapp.voice_recorder.data.database.entity.TrashFileEntity
import com.eva.recorderapp.voice_recorder.data.recordings.utils.toEntity
import com.eva.recorderapp.voice_recorder.data.recordings.utils.toModel
import com.eva.recorderapp.voice_recorder.data.util.toMillis
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.domain.recordings.models.TrashRecordingModel
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.ResourcedTrashRecordingModels
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.TrashRecordingsProvider
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.TrashVoiceRecordings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File
import java.io.IOException
import kotlin.time.Duration.Companion.days

private const val TAG = "TRASH_RECORDING_PROVIDER"
private const val FOLDER_NAME = "trash_recordings"

class TrashRecordingsProviderApi29Impl(
	private val context: Context,
	private val trashMediaDao: TrashFileDao,
	private val recordingsDao: RecordingsMetadataDao,
) : RecordingsProvider(context), TrashRecordingsProvider {

	private val filesDir: File
		get() = File(context.filesDir, FOLDER_NAME).apply(File::mkdirs)

	private val thirtyDaysLater: LocalDateTime
		get() = Clock.System.now().plus(30.days)
			.toLocalDateTime(TimeZone.currentSystemDefault())

	override val trashedRecordingsFlow: Flow<ResourcedTrashRecordingModels>
		get() = trashMediaDao.getAllTrashFilesFlow()
			.flowOn(Dispatchers.IO)
			.map { entries ->
				val result = entries.map(TrashFileEntity::toModel)
				Resource.Success<TrashVoiceRecordings, Nothing>(result)
			}

	override suspend fun getTrashedVoiceRecordings(): ResourcedTrashRecordingModels {
		return withContext(Dispatchers.IO) {
			try {
				val trashedItems = trashMediaDao.getAllTrashFiles()
					.map(TrashFileEntity::toModel)
				Resource.Success(trashedItems)
			} catch (e: SQLException) {
				Resource.Error(e, "SQL EXCEPTION")
			} catch (e: IOException) {
				Resource.Error(e)
			}
		}
	}

	override suspend fun restoreRecordingsFromTrash(recordings: Collection<TrashRecordingModel>): Resource<Unit, Exception> {
		return withContext(Dispatchers.IO) {
			supervisorScope {
				try {
					val operations = recordings.map { model ->
						val entity = model.toEntity()
						// async
						async(Dispatchers.IO) {
							// restore from internal storage
							val isRestoreOk = restoreRecordingsDataFromTableAndFile(entity)
							// delete from internal storage
							val isDeleteOk = deleteRecordingInfoFromFileAndTable(entity)
							//create new  metadata
							recordingsDao.updateOrInsertRecordingMetadata(RecordingsMetaDataEntity(entity.id))
							isRestoreOk && isDeleteOk
						}
					}
					// run all the operations together
					operations.awaitAll()
					Log.d(TAG, "TRASHED ITEMS RECOVERED")
					Resource.Success(
						data = Unit,
						message = context.getString(R.string.restore_recordings_success)
					)
				} catch (e: Exception) {
					Resource.Error(e)
				}
			}
		}
	}

	override suspend fun createTrashRecordings(recordings: Collection<RecordedVoiceModel>): Resource<Unit, Exception> {
		return withContext(Dispatchers.IO) {
			try {
				// perform operations with creating a backup file
				val operations = recordings.map { recording ->
					async {
						// backup file
						val tableEntry = createBackupFileForRecording(recording)
						// add metadata to table
						trashMediaDao.addNewTrashFile(tableEntry)
						// delete the current recording info
						val isSuccess = deleteCurrentRecordingFromStorage(recording)
						if (!isSuccess) {
							Log.d(TAG, "FAILED TO DELETE FILE FROM THE STORAGE")
							Log.d(TAG, "REMOVING FILE FROM TRASH DATABASE")
							// if this failed delete the file and the table entry
							deleteRecordingInfoFromFileAndTable(tableEntry)
						}
					}
				}
				// run all the operations together
				operations.awaitAll()
				Log.d(TAG, "REMOVED SECONDARY METADATA")
				Resource.Success(Unit)
			} catch (e: Exception) {
				Resource.Error(e)
			}
		}
	}

	override suspend fun onPostTrashRecordings(recordings: Collection<RecordedVoiceModel>): Resource<Unit, Exception> {
		return try {
			withContext(Dispatchers.IO) {
				// remove the secondary metadata
				val ids = recordings.map { it.id }
				// now remove secondary data
				recordingsDao.deleteRecordingMetaDataFromIds(ids)
			}
			Resource.Success(Unit)
		} catch (e: SQLException) {
			Resource.Error(e)
		} catch (e: Exception) {
			Resource.Error(e)
		}
	}

	override suspend fun permanentlyDeleteRecordingsInTrash(
		trashRecordings: Collection<TrashRecordingModel>
	): Resource<Unit, Exception> {
		return withContext(Dispatchers.IO) {
			supervisorScope {
				try {
					val operations = trashRecordings.map(TrashRecordingModel::toEntity)
						.map { entity ->
							// delete from internal storage
							async(Dispatchers.IO) { deleteRecordingInfoFromFileAndTable(entity) }
						}
					// run all the operations together
					val isAllGood = operations.awaitAll().all { it }
					Log.d(TAG, "PERMANENT REMOVED FILES FROM TRASH : RESULT :$isAllGood")
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


	private suspend fun restoreRecordingsDataFromTableAndFile(entity: TrashFileEntity): Boolean {
		return withContext(Dispatchers.IO) {
			val file = entity.file.toUri().toFile()

			if (!file.exists()) {
				Log.d(TAG, "FILE DO-NOT EXISTS")
				return@withContext false
			}

			val newUri = contentResolver.insert(volumeUri, entity.createContentValues)
				?: kotlin.run {
					Log.d(TAG, "UNABLE TO CREATE A URI")
					return@withContext false
				}

			val copyFileJob = launch(Dispatchers.IO) {
				contentResolver.openOutputStream(newUri, "w")?.use { stream ->
					// read the bytes and submit to the new uri
					val data = file.readBytes()
					stream.write(data)
					Log.d(TAG, "WRITTEN DATA FOR DATA : ${entity.id}")
				}
			}
			copyFileJob.join()

			val updateMetaData = ContentValues().apply {
				put(MediaStore.Audio.AudioColumns.IS_PENDING, 0)
				put(MediaStore.Audio.AudioColumns.DATE_MODIFIED, System.currentTimeMillis())
			}

			val isSuccess = contentResolver.update(newUri, updateMetaData, null, null)
			isSuccess == 1
		}
	}

	private suspend fun deleteRecordingInfoFromFileAndTable(entity: TrashFileEntity): Boolean {
		return withContext(Dispatchers.IO) {
			try {
				val file = entity.file.toUri().toFile()
				if (file.exists()) {
					val result = file.delete()
					Log.d(TAG, "FILE REMOVE RESULT $result")
				}
				//delete the entry
				trashMediaDao.deleteTrashEntity(entity)
				Log.d(TAG, "REMOVED ENTRY ")
				true
			} catch (e: Exception) {
				e.printStackTrace()
				false
			}
		}
	}

	private val TrashFileEntity.createContentValues: ContentValues
		get() = ContentValues().apply {
			put(MediaStore.Audio.AudioColumns.RELATIVE_PATH, recordingsMusicDirPath)
			put(MediaStore.Audio.AudioColumns.TITLE, title)
			put(MediaStore.Audio.AudioColumns.DISPLAY_NAME, displayName)
			put(MediaStore.Audio.AudioColumns.MIME_TYPE, mimeType)
			put(MediaStore.Audio.AudioColumns.DATE_ADDED, dateAdded.toMillis())
			put(MediaStore.Audio.AudioColumns.DATE_TAKEN, dateAdded.toMillis())
			put(MediaStore.Audio.AudioColumns.ARTIST, context.packageName)
			put(MediaStore.Audio.AudioColumns.IS_PENDING, 1)
		}


	private suspend fun createBackupFileForRecording(
		recording: RecordedVoiceModel,
		expiry: LocalDateTime = thirtyDaysLater,
	): TrashFileEntity {
		return withContext(Dispatchers.IO) {

			val recordingUri = recording.fileUri.toUri()
			val trashFile = File(filesDir, "file_${recording.id}")

			try {
				val operation = async(Dispatchers.IO) {
					// adding the bytes info to a separate file
					context.contentResolver.openInputStream(recordingUri)?.use { stream ->
						val bytes = stream.readBytes()
						// upload the contents to a new file
						trashFile.writeBytes(bytes)
					}
					trashFile
				}

				return@withContext recording.toEntity(
					expires = expiry,
					fileUri = operation.await().toUri().toString()
				)
			} catch (e: CancellationException) {
				// if cancel delete the file then throw the cancellation exception
				if (trashFile.exists()) trashFile.delete()
				throw e
			}
		}
	}

	private suspend fun deleteCurrentRecordingFromStorage(recording: RecordedVoiceModel): Boolean {
		val uri = recording.fileUri.toUri()

		val (fileTrashed, _) = checkIfUriAlreadyTrashedAndNotPending(uri)
		// if the file is already trashed no need to perform delete
		if (fileTrashed) {
			Log.d(TAG, "RECORD IS ALREADY TRASHED CANNOT REMOVE IT ")
			return false
		}
		// if delete is success then it should result in one row change
		return permanentDeleteFromStorage(uri)
	}
}

