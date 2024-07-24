package com.eva.recorderapp.voice_recorder.data.files

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.eva.recorderapp.R
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.data.database.TrashFileMetaDataEntity
import com.eva.recorderapp.voice_recorder.data.database.TrashFilesMetaDataDao
import com.eva.recorderapp.voice_recorder.data.mapper.toEntity
import com.eva.recorderapp.voice_recorder.data.mapper.toModel
import com.eva.recorderapp.voice_recorder.domain.files.ResourcedTrashRecordingModels
import com.eva.recorderapp.voice_recorder.domain.files.TrashRecordingsProvider
import com.eva.recorderapp.voice_recorder.domain.files.TrashVoiceRecordings
import com.eva.recorderapp.voice_recorder.domain.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.domain.models.TrashRecordingModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File
import java.io.IOException
import kotlin.time.Duration.Companion.days

private const val TAG = "TRASH_RECORDING_PROVIDER"

class TrashRecordingsProviderApi29Impl(
	private val context: Context,
	private val trashMediaDao: TrashFilesMetaDataDao
) : RecordingsUtils(context), TrashRecordingsProvider {

	private val filesDir: File
		get() = File(context.filesDir, FOLDER_NAME).apply(File::mkdirs)

	override val trashedRecordingsFlow: Flow<ResourcedTrashRecordingModels>
		get() = trashMediaDao.getAllTrashFilesFlow()
			.map { entires ->
				val result = entires.map(TrashFileMetaDataEntity::toModel)
				Resource.Success<TrashVoiceRecordings, Nothing>(result)
			}
			.flowOn(Dispatchers.IO)

	override suspend fun getTrashedVoiceRecordings(): ResourcedTrashRecordingModels {
		return withContext(Dispatchers.IO) {
			try {
				val trashedItems = trashMediaDao.getAllTrashFiles()
					.map(TrashFileMetaDataEntity::toModel)
				Resource.Success(trashedItems)
			} catch (e: SQLException) {
				Resource.Error(e, "SQL EXCEPTION")
			} catch (e: IOException) {
				Resource.Error(e)
			}
		}
	}

	override suspend fun restoreRecordingsFromTrash(trashRecordings: Collection<TrashRecordingModel>)
			: Resource<Unit, Exception> {
		return supervisorScope {
			try {
				val operations = trashRecordings.map(TrashRecordingModel::toEntity).map { entity ->
					async(Dispatchers.IO) {
						// restore from internal storage
						val isRestoreOk = restoreRecordingsDataFromTableAndFile(entity)
						// delete from internal storage
						val isDeleteOk = deleteRecordingInfoFromFileAndTable(entity)
						isRestoreOk && isDeleteOk
					}
				}
				// run all the operations together
				val isAllGood = operations.awaitAll().all { it }
				Log.d(TAG, "TRASHED ITEMS RECOVERED $isAllGood")
				Resource.Success(Unit)
			} catch (e: Exception) {
				Resource.Error(e)
			}
		}
	}

	override suspend fun createTrashRecordings(recordings: Collection<RecordedVoiceModel>): Resource<Unit, Exception> {
		return supervisorScope {
			try {
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
							Log.d(TAG, "REMOVEING FILE FROM TRASH DATABASE")
							// if this failed delete the file and the table entry
							deleteRecordingInfoFromFileAndTable(tableEntry)
						}
					}
				}
				// run all the operations together
				operations.awaitAll()
				Resource.Success(Unit)
			} catch (e: Exception) {
				Resource.Error(e)
			}
		}
	}

	override suspend fun permanentlyDeleteRecordedVoicesInTrash(trashRecordings: Collection<TrashRecordingModel>): Resource<Unit, Exception> {
		return withContext(Dispatchers.IO) {
			supervisorScope {
				try {
					val operations = trashRecordings.map(TrashRecordingModel::toEntity).map { entity ->
						async(Dispatchers.IO) {
							// delete from internal storage
							deleteRecordingInfoFromFileAndTable(entity)
						}
					}
					// run all the operations together
					val isAllGood = operations.awaitAll().all { it }
					Log.d(TAG, "PERMANENELTY REMOVED FILES FROM TRASH : RESULT :$isAllGood")
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


	suspend private fun restoreRecordingsDataFromTableAndFile(entity: TrashFileMetaDataEntity): Boolean {
		return withContext(Dispatchers.IO) {
			try {

				val metaData = contentValuesFromTrashMetaData(entity)
				val file = entity.file.toUri().toFile()

				if (!file.exists()) {
					Log.d(TAG, "FILE DO-NOT EXISTS")
					return@withContext false
				}

				val newUri = contentResolver.insert(volumeUri, metaData) ?: kotlin.run {
					Log.d(TAG, "UNABLE TO CREATE A URI")
					return@withContext false
				}

				val operation = async(Dispatchers.IO) {
					contentResolver.openOutputStream(newUri, "w")?.use { stream ->
						// read the bytes and submit to the new uri
						val data = file.readBytes()
						stream.write(data)
						Log.d(TAG, "WRITTEN DATA FOR DATA : ${entity.id}")
					}
				}
				operation.await()

				val updateMetaData = ContentValues().apply {
					put(MediaStore.Audio.AudioColumns.IS_PENDING, 0)
					put(MediaStore.Audio.AudioColumns.DATE_MODIFIED, System.currentTimeMillis())
				}

				val isSuccess = contentResolver.update(newUri, updateMetaData, null, null)
				isSuccess == 1
			} catch (e: SQLException) {
				Log.e(TAG, "SQL EXCEPTION")
				throw e
			} catch (e: Exception) {
				e.printStackTrace()
				throw e
			}
		}
	}

	suspend fun deleteRecordingInfoFromFileAndTable(entity: TrashFileMetaDataEntity): Boolean {
		return withContext(Dispatchers.IO) {
			try {
				val file = entity.file.toUri().toFile()
				if (file.exists()) {
					Log.d(TAG, "FILE FOUND")
					// delete the file
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

	suspend private fun contentValuesFromTrashMetaData(
		trash: TrashFileMetaDataEntity,
		isNewId: Boolean = false
	): ContentValues {
		return ContentValues().apply {
			if (!isNewId) {
				put(MediaStore.Audio.AudioColumns._ID, trash.id)
			}
			put(MediaStore.Audio.AudioColumns.RELATIVE_PATH, musicDir)
			put(MediaStore.Audio.AudioColumns.TITLE, trash.title)
			put(MediaStore.Audio.AudioColumns.DISPLAY_NAME, trash.displayName)
			put(MediaStore.Audio.AudioColumns.MIME_TYPE, trash.mimeType)
			put(
				MediaStore.Audio.AudioColumns.DATE_ADDED,
				trash.dateAdded.toMilliSeconds()
			)
			put(
				MediaStore.Audio.AudioColumns.DATE_TAKEN,
				trash.dateAdded.toMilliSeconds()
			)
			put(MediaStore.Audio.AudioColumns.ARTIST, context.packageName)
			put(MediaStore.Audio.AudioColumns.IS_PENDING, 1)
		}
	}


	suspend private fun createBackupFileForRecording(recording: RecordedVoiceModel): TrashFileMetaDataEntity {
		return withContext(Dispatchers.IO) {

			val recordingUri = recording.fileUri.toUri()
			val trashFile = File(filesDir, "file_${recording.id}")

			try {
				val operation = async(Dispatchers.IO) {
					// adding the bytes info to a seperate file
					context.contentResolver.openInputStream(recordingUri)?.use { stream ->
						val bytes = stream.readBytes()
						// upload the contents to a new file
						trashFile.writeBytes(bytes)
					}
					trashFile
				}

				val fileUri = operation.await().toUri().toString()

				// change the date of expiry if needed
				val expiredOn = Clock.System.now().plus(30.days)
					.toLocalDateTime(TimeZone.currentSystemDefault())

				return@withContext recording.toEntity(expires = expiredOn, fileUri = fileUri)
			} catch (e: CancellationException) {
				// if cancel delete the file then throw the cancellation exception
				if (trashFile.exists()) trashFile.delete()
				throw e
			}
		}
	}

	suspend private fun deleteCurrentRecordingFromStorage(recording: RecordedVoiceModel): Boolean {
		val uri = recording.fileUri.toUri()

		val fileTrashed = checkIfUriAlreadyTrashedAndNotPending(uri)
		// if the file is already trashed no need to perform delete
		if (fileTrashed) {
			Log.d(TAG, "RECORD IS ALREADY TRASHED CANNOT REMOVE IT ")
			return false
		}
		// if delete is success then it should result in one row change
		return permanentDeleteFromStorage(uri)
	}

	companion object {
		const val FOLDER_NAME = "trash_recordings"
	}

}

