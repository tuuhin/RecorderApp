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

	private val filesDir: File
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
		// ensure that only this app files are restore.
		return coroutineScope {
			try {
				// recordings are from here only so no need to check for owner
				val operations = recordings.map { model ->
					val entity = model.toEntity()
					Log.d(TAG, "WORKING FOR $entity")
					// async
					async(Dispatchers.IO) {
						// restore from internal storage
						restoreRecordingsDataFromTableAndFile(entity)
						// delete from internal storage
						deleteRecordingInfoFromFileAndTable(entity)
						Unit
					}
				}
				// run all the operations together
				operations.awaitAll()

				val createSecondaryMetadata = recordings.map { model ->
					RecordingsMetaDataEntity(model.id)
				}
				withContext(Dispatchers.IO) {
					recordingsDao.updateOrInsertRecordingMetadataBulk(createSecondaryMetadata)
				}

				Log.d(TAG, "TRASHED ITEMS RECOVERED")

				val message = context.getString(R.string.restore_recordings_success)
				Resource.Success(Unit, message)
			} catch (e: Exception) {
				Resource.Error(e)
			}
		}
	}


	override fun createTrashRecordings(recordings: Collection<RecordedVoiceModel>)
			: Flow<Resource<Collection<RecordedVoiceModel>, Exception>> {

		val recordingsWithOwnerShip = recordings.filter { it.owner == context.packageName }
		val recordingsWithoutOwnerShip = recordings.filterNot { it.owner == context.packageName }

		return flow<Resource<Collection<RecordedVoiceModel>, Exception>> {

			if (recordingsWithOwnerShip.isNotEmpty()) {
				try {
					coroutineScope {
						// perform operations with creating a backup file
						val operations = recordingsWithOwnerShip.map { recording ->
							async(Dispatchers.IO) {
								createBackUpEntry(recording)
							}
						}
						// run all the operations together
						operations.awaitAll()
						// now remove secondary data
						val ids = recordings.map { it.id }
						withContext(Dispatchers.IO) {
							recordingsDao.deleteRecordingMetaDataFromIds(ids)
						}
					}
					val successMessage = context.getString(R.string.recording_trash_request_success)
					emit(Resource.Success(emptyList(), message = successMessage))
				} catch (e: Exception) {
					emit(Resource.Error(e))
				}
			}
			if (recordingsWithoutOwnerShip.isNotEmpty()) {
				emit(Resource.Error(CannotTrashFileDifferentOwnerException()))
			}
		}.flowOn(Dispatchers.IO)
	}

	override suspend fun permanentlyDeleteRecordingsInTrash(trashRecordings: Collection<TrashRecordingModel>)
			: Resource<Unit, Exception> {
		return coroutineScope {
			try {
				// delete from internal storage
				val operations = trashRecordings.map { model ->
					async(Dispatchers.IO) {
						// delete the file and the table
						deleteRecordingInfoFromFileAndTable(model.toEntity())
					}
				}
				// run all the operations together
				operations.awaitAll()
				Log.d(TAG, "PERMANENT REMOVED FILES FROM TRASH:")
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

	private suspend fun createBackUpEntry(recording: RecordedVoiceModel) {
		val entry = createBackupFileForRecording(recording, thirtyDaysLater)
		// add metadata to table
		trashMediaDao.addNewTrashFile(entry)
		val isDeleteSuccess = try {
			// delete the current recording info
			permanentDeleteFromStorage(recording.fileUri.toUri())
		} catch (e: CancellationException) {
			// if delete was cancelled then delete the file and the table entry
			withContext(NonCancellable) {
				Log.d(TAG, "FAILED TO DELETE FILE FROM THE STORAGE")
				deleteRecordingInfoFromFileAndTable(entry)
			}
			throw e
		}

		if (!isDeleteSuccess) {
			// if delete is unsuccessful so delete the file and the entry
			deleteRecordingInfoFromFileAndTable(entry)
		}
	}


	private suspend fun restoreRecordingsDataFromTableAndFile(entity: TrashFileEntity) {
		return coroutineScope {
			val file = entity.file.toUri().toFile()

			if (!file.exists()) {
				Log.d(TAG, "FILE DO-NOT EXISTS")
				return@coroutineScope
			}

			val metaData = ContentValues().apply {
				put(MediaStore.Audio.AudioColumns.RELATIVE_PATH,
					RecordingsConstants.RECORDINGS_MUSIC_PATH
				)
				put(MediaStore.Audio.AudioColumns.DISPLAY_NAME, entity.displayName)
				put(MediaStore.Audio.AudioColumns.MIME_TYPE, entity.mimeType)
				put(MediaStore.Audio.AudioColumns.DATE_ADDED, epochSeconds)
				put(MediaStore.Audio.AudioColumns.IS_PENDING, 1)
			}

			val newUri = withContext(Dispatchers.IO) {
				contentResolver.insert(RecordingsConstants.AUDIO_VOLUME_URI, metaData)
			} ?: return@coroutineScope

			withContext(Dispatchers.IO) {
				contentResolver.openOutputStream(newUri, "w")?.use { stream ->
					// read the bytes and submit to the new uri
					val data = file.readBytes()
					stream.write(data)
					Log.d(TAG, "WRITTEN DATA FOR DATA : ${entity.id}")
				}
			}

			val updateMetaData = ContentValues().apply {
				put(MediaStore.Audio.AudioColumns.IS_PENDING, 0)
				put(MediaStore.Audio.AudioColumns.DATE_MODIFIED, System.currentTimeMillis())
			}

			withContext(Dispatchers.IO) {
				contentResolver.update(newUri, updateMetaData, null, null)
			}

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
	): TrashFileEntity {
		return coroutineScope {

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

				return@coroutineScope recording.toEntity(
					expires = expiry,
					fileUri = operation.await().toUri().toString()
				)
			} catch (e: CancellationException) {
				// if cancel delete the file then throw the cancellation exception
				withContext(NonCancellable) {
					try {
						if (trashFile.exists()) trashFile.delete()
						else Unit
					} catch (e: IOException) {
						Log.e(TAG, "FAILED TO REMOVE THE FILE", e)
					}
				}
				throw e
			}
		}
	}
}

