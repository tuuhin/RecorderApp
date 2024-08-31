package com.eva.recorderapp.voice_recorder.data.recorder

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.data.recordings.files.RecordingsUtils
import com.eva.recorderapp.voice_recorder.domain.datastore.enums.AudioFileNamingFormat
import com.eva.recorderapp.voice_recorder.domain.datastore.repository.RecorderFileSettingsRepo
import com.eva.recorderapp.voice_recorder.domain.recorder.RecorderFileProvider
import com.eva.recorderapp.voice_recorder.domain.recorder.models.RecordEncoderAndFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

private const val LOGGER_TAG = "RECORDER_FILE_PROVIDE"

class RecorderFileProviderImpl(
	private val context: Context,
	private val settings: RecorderFileSettingsRepo,
) : RecordingsUtils(context), RecorderFileProvider {


	override suspend fun createFileForRecoring(extension: String?): File {
		return withContext(Dispatchers.IO) {
			val file = File.createTempFile("recording", extension, context.cacheDir)
			Log.d(LOGGER_TAG, "FILE CREATED FOR RECORDING NAME: ${file.name}")
			file
		}
	}

	override suspend fun transferFileDataToStorage(file: File, format: RecordEncoderAndFormat)
			: Boolean {
		return withContext(Dispatchers.IO) {

			try {
				// file don't exits
				if (!file.exists()) return@withContext false
				// content uri cannot be created
				val contenUri = createUriForRecording(format) ?: return@withContext false

				Log.d(LOGGER_TAG, "UPDATING THE FILE CONTENT..")
				val updateContent = async(Dispatchers.IO) {
					contentResolver.openOutputStream(contenUri, "w")?.use { stream ->
						stream.write(file.readBytes())
					}
				}
				// wait for the file data to be completely read
				updateContent.await()
				// update the meta data for the file
				updateUriAfterRecording(contenUri)
				return@withContext true
			} catch (e: IllegalArgumentException) {
				Log.e(LOGGER_TAG, "EXTRAS PROVIDED WRONG")
				false
			} catch (e: IOException) {
				Log.e(LOGGER_TAG, "IO EXCEPTION", e)
				e.printStackTrace()
				false
			}
		}
	}

	override suspend fun deleteCreatedFile(file: File): Boolean {
		return withContext(Dispatchers.IO) {
			if (!file.exists()) return@withContext false
			val result = file.delete()
			Log.d(LOGGER_TAG, "TEMP FILE DELETED : $result")
			result
		}
	}

	private suspend fun createUriForRecording(format: RecordEncoderAndFormat): Uri? {
		return try {
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
				put(MediaStore.Audio.AudioColumns.MIME_TYPE, format.mimeType)
				put(MediaStore.Audio.AudioColumns.DATE_ADDED, epochSeconds)
				put(MediaStore.Audio.AudioColumns.IS_PENDING, 1)
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
					put(MediaStore.Audio.AudioColumns.IS_RECORDING, 1)
				}
			}

			Log.d(LOGGER_TAG, "CREATING FILE")
			val contentUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				contentResolver.insert(volumeUri, metaData, null)
			} else {
				contentResolver.insert(volumeUri, metaData)
			}
			Log.d(LOGGER_TAG, "URI CREATED , $contentUri")
			contentUri
		} catch (e: IllegalArgumentException) {
			Log.e(LOGGER_TAG, "EXTRAS PROVIDED WRONG", e)
			null
		} catch (e: IOException) {
			Log.e(LOGGER_TAG, "IO EXCEPTION", e)
			e.printStackTrace()
			null
		}
	}

	private suspend fun updateUriAfterRecording(file: Uri): Resource<Unit, Exception> {
		return try {
			val updatedMetaData = ContentValues().apply {
				put(MediaStore.Audio.AudioColumns.IS_PENDING, 0)
				put(MediaStore.Audio.AudioColumns.DATE_MODIFIED, epochSeconds)
			}
			withContext(Dispatchers.IO) {
				contentResolver.update(file, updatedMetaData, null, null)
			}
			Log.d(LOGGER_TAG, "UPDATED URI AFTER RECORDING")
			// single row affected i.e, the updated file
			Resource.Success(Unit)
		} catch (e: SecurityException) {
			Resource.Error(e, "Security issues")
		} catch (e: SQLException) {
			Resource.Error(e, "SQL_ERROR")
		} catch (e: IOException) {
			e.printStackTrace()
			Resource.Error(e)
		}
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

