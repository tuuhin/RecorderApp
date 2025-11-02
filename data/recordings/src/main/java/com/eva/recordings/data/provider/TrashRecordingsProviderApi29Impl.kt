package com.eva.recordings.data.provider

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.eva.database.dao.RecordingsMetadataDao
import com.eva.database.dao.TrashFileDao
import com.eva.database.entity.RecordingsMetaDataEntity
import com.eva.database.entity.TrashFileEntity
import com.eva.recordings.R
import com.eva.recordings.data.utils.toEntity
import com.eva.recordings.data.utils.toModel
import com.eva.recordings.data.wrapper.RecordingsConstants
import com.eva.recordings.data.wrapper.RecordingsContentResolverWrapper
import com.eva.recordings.domain.exceptions.CannotTrashFileDifferentOwnerException
import com.eva.recordings.domain.models.RecordedVoiceModel
import com.eva.recordings.domain.models.TrashRecordingModel
import com.eva.recordings.domain.provider.ResourcedTrashRecordingModels
import com.eva.recordings.domain.provider.TrashRecordingsProvider
import com.eva.recordings.domain.provider.TrashVoiceRecordings
import com.eva.utils.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File
import java.io.IOException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

private const val TAG = "TRASH_RECORDING_PROVIDER"

internal class TrashRecordingsProviderApi29Impl(
	private val context: Context,
	private val trashMediaDao: TrashFileDao,
	private val recordingsDao: RecordingsMetadataDao,
) : RecordingsContentResolverWrapper(context), TrashRecordingsProvider {

	private val folderName = "trash_recordings"

	private val trashFilesDirectory: File
		get() = File(context.filesDir, folderName).apply(File::mkdirs)

	@OptIn(ExperimentalTime::class)
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
		return try {
			// recordings are from here only so no need to check for owner
			supervisorScope {
				val operations = recordings.map { model ->
					val entity = model.toEntity()
					// async
					async(Dispatchers.IO) {
						// restore from internal storage
						restoreRecordingsDataFromTableAndFile(entity)
						// delete from internal storage
						removeBackupFileAndMetadata(entity)
					}
				}
				// run all the operations together
				operations.awaitAll()
			}

			val createSecondaryMetadata = recordings.map { model ->
				RecordingsMetaDataEntity(model.id)
			}
			recordingsDao.updateOrInsertRecordingMetadataBulk(createSecondaryMetadata)
			Log.d(TAG, "TRASHED ITEMS RECOVERED")

			val message = context.getString(R.string.restore_recordings_success)
			Resource.Success(Unit, message)
		} catch (e: Exception) {
			Resource.Error(e)
		}
	}

	override fun createTrashRecordings(recordings: Collection<RecordedVoiceModel>)
			: Flow<Resource<Collection<RecordedVoiceModel>, Exception>> {

		val ownerRecordings = recordings.filter { it.owner == context.packageName }
		val notOwnerRecordings = recordings.filterNot { it.owner == context.packageName }

		return flow {
			try {
				supervisorScope {
					// perform operations with creating a backup file
					val operations = ownerRecordings.map { recording ->
						async(Dispatchers.IO) { createBackUpEntry(recording) }
					}
					// run all the operations together
					operations.awaitAll()
				}
				// now remove secondary data
				val ids = recordings.map { it.id }
				recordingsDao.deleteRecordingMetaDataFromIds(ids)
				val successMessage = context.getString(R.string.recording_trash_request_success)
				emit(Resource.Success(emptyList(), message = successMessage))
			} catch (e: Exception) {
				Log.d(TAG, "SOME ERROR IN TRASHING FILES", e)
				emit(Resource.Error(e))
			}
			if (notOwnerRecordings.isNotEmpty()) {
				emit(Resource.Error(CannotTrashFileDifferentOwnerException()))
			}
		}
	}

	override fun permanentlyDeleteRecordingsInTrash(trashRecordings: List<TrashRecordingModel>)
			: Flow<Resource<List<TrashRecordingModel>, Exception>> {
		return flow<Resource<List<TrashRecordingModel>, Exception>> {
			try {
				// delete from internal storage
				supervisorScope {
					val operations = trashRecordings.map { model ->
						async(Dispatchers.IO) {
							// delete the file and the table
							removeBackupFileAndMetadata(model.toEntity())
						}
					}
					// run all the operations together
					operations.awaitAll()
				}
				Log.d(TAG, "PERMANENT REMOVED FILES FROM TRASH:")
				emit(
					Resource.Success(
						data = emptyList<TrashRecordingModel>(),
						message = context.getString(R.string.recording_delete_request_success)
					)
				)
			} catch (e: Exception) {
				e.printStackTrace()
				val errorMessage = context.getString(R.string.recording_delete_request_failed)
				emit(Resource.Error(e, errorMessage))
			}
		}.flowOn(Dispatchers.IO)
	}

	override suspend fun permanentlyDeleteRecordings(trashRecordings: List<TrashRecordingModel>): Resource<Unit, Exception> {
		return withContext(Dispatchers.IO) {
			try {
				// move items to trash
				supervisorScope {
					val operations = trashRecordings.map { model ->
						async(Dispatchers.IO) {
							// delete the file and the table
							removeBackupFileAndMetadata(model.toEntity())
						}
					}
					// run all the operations together
					operations.awaitAll()
				}
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

	private suspend fun createBackUpEntry(recording: RecordedVoiceModel) {
		val entry = createBackupFileForRecording(recording, thirtyDaysLater) ?: return
		// add metadata to table
		trashMediaDao.addNewTrashFile(entry)
		val isDeleteSuccess = try {
			// delete the current recording info
			permanentDeleteFromStorage(recording.fileUri.toUri())
		} catch (e: CancellationException) {
			// if delete was cancelled then delete the file and the table entry
			withContext(NonCancellable) {
				Log.d(TAG, "FAILED TO DELETE FILE FROM THE STORAGE")
				removeBackupFileAndMetadata(entry)
			}
			throw e
		}

		if (isDeleteSuccess) return
		// if delete is unsuccessful so delete the file and the entry
		removeBackupFileAndMetadata(entry)
	}


	private suspend fun restoreRecordingsDataFromTableAndFile(entity: TrashFileEntity) {

		val file = entity.file.toUri().toFile()

		if (!file.exists()) return

		val metaData = ContentValues().apply {
			put(
				MediaStore.Audio.AudioColumns.RELATIVE_PATH,
				RecordingsConstants.RECORDINGS_MUSIC_PATH
			)
			put(MediaStore.Audio.AudioColumns.DISPLAY_NAME, entity.displayName)
			put(MediaStore.Audio.AudioColumns.MIME_TYPE, entity.mimeType)
			put(MediaStore.Audio.AudioColumns.DATE_ADDED, epochSeconds)
			put(MediaStore.Audio.AudioColumns.IS_PENDING, 1)
		}

		val updateMetaData = ContentValues().apply {
			put(MediaStore.Audio.AudioColumns.IS_PENDING, 0)
			put(MediaStore.Audio.AudioColumns.DATE_MODIFIED, System.currentTimeMillis())
		}

		try {
			val newUri = withContext(Dispatchers.IO) {
				contentResolver.insert(RecordingsConstants.AUDIO_VOLUME_URI, metaData)
			} ?: return

			try {
				withContext(Dispatchers.IO) {
					contentResolver.openOutputStream(newUri, "w")?.use { stream ->
						// read the bytes and submit to the new uri
						file.inputStream().use { inStream -> inStream.copyTo(stream) }
						Log.d(TAG, "WRITTEN DATA FOR DATA : ${entity.id}")
					}
					contentResolver.update(newUri, updateMetaData, null, null)
				}
			} catch (e: CancellationException) {
				withContext(NonCancellable) {
					contentResolver.delete(newUri, null, null)
					Log.d(TAG, "DELETING NEW URI")
				}
				throw e
			}
		} catch (e: Exception) {
			Log.e(TAG, "ISSUE IN CREATING NEW URI", e)
		}

	}

	private suspend fun removeBackupFileAndMetadata(entity: TrashFileEntity): Boolean {
		return coroutineScope {
			try {
				val fileDeleteJob = async(Dispatchers.IO) {
					try {
						val file = entity.file.toUri().toFile()
						if (!file.exists()) return@async
						val result = file.delete()
						Log.d(TAG, "FILE REMOVE RESULT $result")
						result
					} catch (e: IOException) {
						Log.d(TAG, "TRASH FILE FAILED TO CREATE", e)
					}
				}
				val deleteEntity = async {
					//delete the entry
					trashMediaDao.deleteTrashEntity(entity)
					Log.d(TAG, "REMOVED ENTRY ")
				}
				awaitAll(fileDeleteJob, deleteEntity)
				true
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				e.printStackTrace()
				false
			}
		}
	}


	private suspend fun createBackupFileForRecording(
		recording: RecordedVoiceModel,
		expiry: LocalDateTime,
	): TrashFileEntity? = coroutineScope {
		val recordingUri = recording.fileUri.toUri()
		try {
			val trashFile = withContext(Dispatchers.IO) {
				File(trashFilesDirectory, "file_${recording.id}")
					.apply(File::createNewFile)
			}
			try {
				val copyJob = launch(Dispatchers.IO) {
					context.contentResolver.openInputStream(recordingUri)?.use { inStream ->
						// upload the contents to a new file
						trashFile.outputStream().use { outStream -> inStream.copyTo(outStream) }
					}
				}
				copyJob.join()
				recording.toEntity(
					expires = expiry,
					fileUri = trashFile.toUri().toString()
				)
			} catch (e: CancellationException) {
				withContext(NonCancellable) {
					try {
						trashFile.delete()
					} catch (e: IOException) {
						Log.e(TAG, "FAILED TO REMOVE THE FILE", e)
					}
				}
				throw e
			}
		} catch (e: IOException) {
			Log.e(TAG, "CANNOT CREATE BACKUP FILE", e)
			null
		}
	}
}

