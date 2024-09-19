package com.eva.recorderapp.voice_recorder.data.recordings.provider

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import com.eva.recorderapp.voice_recorder.data.database.dao.RecordingsMetadataDao
import com.eva.recorderapp.voice_recorder.data.database.entity.RecordingsMetaDataEntity
import com.eva.recorderapp.voice_recorder.domain.datastore.enums.AudioFileNamingFormat
import com.eva.recorderapp.voice_recorder.domain.datastore.repository.RecorderFileSettingsRepo
import com.eva.recorderapp.voice_recorder.domain.recorder.RecorderFileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

private const val LOGGER_TAG = "RECORDER_FILE_PROVIDE"

class RecorderFileProviderImpl(
	private val context: Context,
	private val recordingDao: RecordingsMetadataDao,
	private val settings: RecorderFileSettingsRepo,
) : RecordingsProvider(context), RecorderFileProvider {

	private val tempPrefix = "RECORDING"

	override suspend fun createFileForRecording(extension: String?): File {
		return withContext(Dispatchers.IO) {
			val file = File.createTempFile(tempPrefix, extension, context.cacheDir)
			Log.d(LOGGER_TAG, "FILE CREATED FOR RECORDING NAME: ${file.name}")
			file
		}
	}

	override suspend fun transferFileDataToStorage(file: File, mimeType: String): Long? {
		return withContext(Dispatchers.IO) {
			try {
				// file don't exits
				if (!file.exists()) {
					Log.d(LOGGER_TAG, "FILE DON'T EXIT'S")
					return@withContext null
				}
				// content uri cannot be created
				val contentUri = createUriForRecording(mimeType) ?: run {
					Log.d(LOGGER_TAG, "CANNOT CREATE URI FOR RECORDING")
					return@withContext null
				}

				Log.d(LOGGER_TAG, "UPDATING THE FILE CONTENT..")
				val job = launch(Dispatchers.IO) {
					contentResolver.openOutputStream(contentUri, "w")?.use { stream ->
						stream.write(file.readBytes())
					}
				}
				// wait for the file data to be completely read
				job.join()
				val uriId = ContentUris.parseId(contentUri)
				// update the metadata for the file
				val mediaStoreUpdate = async(Dispatchers.IO) {
					updateUriAfterRecording(contentUri)
				}
				val delTempFile = async(Dispatchers.IO) {
					deleteCreatedFile(file)
				}
				// save the secondary metadata
				val otherMetadataDataUpdate = async(Dispatchers.IO) {
					val entity = RecordingsMetaDataEntity(recordingId = uriId)
					recordingDao.updateOrInsertRecordingMetadata(entity)
				}
				// execute them parallel
				Log.d(LOGGER_TAG, "UPDATING METADATA")
				awaitAll(mediaStoreUpdate, otherMetadataDataUpdate, delTempFile)
				return@withContext uriId
			} catch (e: IllegalArgumentException) {
				Log.e(LOGGER_TAG, "EXTRAS PROVIDED WRONG")
			} catch (e: IOException) {
				e.printStackTrace()
			}
			return@withContext null
		}
	}

	override suspend fun deleteCreatedFile(file: File): Boolean {
		return withContext(Dispatchers.IO) {
			// ensure the file exits  and starts with the tempPrefix
			if (!file.exists() && !file.name.startsWith(tempPrefix, true)) return@withContext false
			val result = file.delete()
			Log.d(LOGGER_TAG, "TEMP FILE DELETED : $result")
			result
		}
	}

	private suspend fun createUriForRecording(mimeType: String): Uri? {

		val fileSettings = settings.fileSettings
		val name = fileSettings.name
		val nameFormat = fileSettings.format
		val identifier = when (nameFormat) {
			AudioFileNamingFormat.DATE_TIME -> "$epochSeconds"
			AudioFileNamingFormat.COUNT -> {
				val currentCount = getItemNumber()
				"${currentCount + 1}".padStart(3, '0')
			}
		}

		// file name
		val fileName = buildString {
			append(name)
			append("_")
			append(identifier)
		}

		val metaData = ContentValues().apply {
			put(MediaStore.Audio.AudioColumns.RELATIVE_PATH, recordingsMusicDirPath)
			put(MediaStore.Audio.AudioColumns.DISPLAY_NAME, fileName)
			put(MediaStore.Audio.AudioColumns.MIME_TYPE, mimeType)
			put(MediaStore.Audio.AudioColumns.DATE_ADDED, epochSeconds)
			put(MediaStore.Audio.AudioColumns.IS_PENDING, 1)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				put(MediaStore.Audio.AudioColumns.IS_RECORDING, 1)
			}
		}

		Log.d(LOGGER_TAG, "CREATING FILE")
		val contentUri = withContext(Dispatchers.IO) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				contentResolver.insert(volumeUri, metaData, null)
			} else {
				contentResolver.insert(volumeUri, metaData)
			}
		}
		Log.d(LOGGER_TAG, "URI CREATED , $contentUri")

		return contentUri
	}

	private suspend fun updateUriAfterRecording(file: Uri): Boolean {

		val updatedMetaData = ContentValues().apply {
			put(MediaStore.Audio.AudioColumns.IS_PENDING, 0)
			put(MediaStore.Audio.AudioColumns.DATE_MODIFIED, epochSeconds)
		}
		val result = withContext(Dispatchers.IO) {
			contentResolver.update(file, updatedMetaData, null, null)
		}
		Log.d(LOGGER_TAG, "UPDATED URI AFTER RECORDING")

		return result == 1
	}

	/**
	 * Method used to get a unique no. to give the file name should be short
	 */
	private suspend fun getItemNumber(): Int {
		val projection = arrayOf(MediaStore.Audio.AudioColumns._ID)
		val selection = "${MediaStore.Audio.AudioColumns.OWNER_PACKAGE_NAME} = ?"
		val selectionArgs = arrayOf(context.packageName)

		val bundle = Bundle().apply {
			putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
			putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
		}

		return withContext(Dispatchers.IO) {
			contentResolver.query(volumeUri, projection, bundle, null)
				?.use { cursor -> cursor.count }
				?: 0
		}
	}
}