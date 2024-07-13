package com.eva.recorderapp.data.voice_recorder

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.database.getIntOrNull
import androidx.media3.common.MimeTypes
import com.eva.recorderapp.domain.models.RecordedVoiceModel
import kotlinx.datetime.Clock
import java.io.File
import com.eva.recorderapp.domain.voice_recorder.RecorderFileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.IOException

private const val LOGGER_TAG = "RECORDER_FILE_LOGGER"

class RecorderFileProviderImpl(
	private val context: Context
) : RecorderFileProvider {

	private val contentResolver
		get() = context.contentResolver

	// package name to store the name
	// TODO: Allow changing the directory later
	private val recordingDir: String
		get() = Environment.DIRECTORY_MUSIC + File.separator + context.packageName

	private val baseVolumeUri: Uri
		get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
			MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
		else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI


	override suspend fun getAppVoiceRecordings(): List<RecordedVoiceModel> {
		val projection = arrayOf(
			MediaStore.Audio.AudioColumns._ID,
			MediaStore.Audio.AudioColumns.TITLE,
			MediaStore.Audio.AudioColumns.DISPLAY_NAME,
			MediaStore.Audio.AudioColumns.DURATION,
			MediaStore.Audio.AudioColumns.SIZE,
			MediaStore.Audio.AudioColumns.DATE_MODIFIED,
			MediaStore.Audio.AudioColumns.DATE_ADDED,
		)
		val sortOrder = "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"
		val selection = "${MediaStore.Audio.Media.RELATIVE_PATH} = $recordingDir"

		return withContext(Dispatchers.IO) {
			try {
				contentResolver.query(baseVolumeUri, projection, selection, null, sortOrder)
					?.use { cursor ->
						val idColumn =
							cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns._ID)
						val titleColumn =
							cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE)
						val sizeColum =
							cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.SIZE)
						val nameColumn =
							cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DISPLAY_NAME)
						val durationColumn =
							cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION)
						val dateUpdatedColumn =
							cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATE_MODIFIED)
						val dateCreatedColumn =
							cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATE_ADDED)

						return@withContext buildList<RecordedVoiceModel> {
							while (cursor.moveToNext()) {
								val id = cursor.getLong(idColumn)
								val title = cursor.getString(titleColumn)
								val displayName = cursor.getString(nameColumn)
								val duration = cursor.getLong(durationColumn)
								val size = cursor.getLong(sizeColum)
								val dateUpdated = cursor.getLong(dateUpdatedColumn)
								val dateAdded = cursor.getLong(dateCreatedColumn)
								val uriString = ContentUris.withAppendedId(baseVolumeUri, id)
									.toString()

								val model = RecordedVoiceModel(
									id = id,
									title = title,
									displayName = displayName,
									duration = duration,
									sizeInBytes = size,
									modifiedAt = dateUpdated.toDateTime(),
									recordedAt = dateAdded.toDateTime(),
									fileUri = uriString
								)
								add(model)
							}
						}
					} ?: emptyList()
			} catch (e: Exception) {
				e.printStackTrace()
				emptyList<RecordedVoiceModel>()
			}
		}
	}

	override suspend fun createFileForRecording(): Uri? {
		// TODO: Allow user to change the audio file name and also format
		val time = Clock.System.now()
		val fileName = "AUD_REC_$time.3gpp"

		val metaData = ContentValues().apply {
			put(MediaStore.Audio.AudioColumns.RELATIVE_PATH, recordingDir)
			put(MediaStore.Audio.AudioColumns.TITLE, fileName)
			put(MediaStore.Audio.AudioColumns.DISPLAY_NAME, fileName)
			put(MediaStore.Audio.AudioColumns.MIME_TYPE, MimeTypes.AUDIO_AMR_NB)
			put(MediaStore.Audio.AudioColumns.DATE_ADDED, System.currentTimeMillis())
			put(MediaStore.Audio.AudioColumns.DATE_TAKEN, System.currentTimeMillis())
			put(MediaStore.Audio.AudioColumns.DATE_MODIFIED, System.currentTimeMillis())
			put(MediaStore.Audio.AudioColumns.IS_PENDING, 1)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				put(MediaStore.Audio.AudioColumns.IS_RECORDING, 1)
			}
		}

		return withContext(Dispatchers.IO) {
			try {
				Log.d(LOGGER_TAG, "CREATING FILE")
				val finalUri = contentResolver.insert(baseVolumeUri, metaData)
				Log.d(LOGGER_TAG, "URI CREATED , $finalUri")
				finalUri
			} catch (e: IOException) {
				Log.e(LOGGER_TAG, "IO EXCEPTION", e)
				e.printStackTrace()
				null
			}
		}
	}

	override suspend fun updateFileAfterRecording(file: Uri): Boolean {
		val updatedMetaData = ContentValues().apply {
			put(MediaStore.Audio.AudioColumns.IS_PENDING, 0)
			put(MediaStore.Audio.AudioColumns.DATE_MODIFIED, System.currentTimeMillis())
		}
		return withContext(Dispatchers.IO) {
			try {
				val rowsEffected = contentResolver.update(file, updatedMetaData, null, null)
				Log.d(LOGGER_TAG, "UPDATED URI AFTER RECORDING")
				// single row affected i.e, the updated file
				return@withContext rowsEffected == 1
			} catch (e: IOException) {
				e.printStackTrace()
				false
			}
		}
	}

	override suspend fun cancelFileCreation(file: Uri): Boolean {
		return withContext(Dispatchers.IO) {
			try {
				val projection = arrayOf(MediaStore.Audio.AudioColumns.IS_PENDING)
				//checking is required to ensure its not pending anymore
				contentResolver.query(file, projection, null, null, null)
					?.use { cursor ->
						val isPendingColumn =
							cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.IS_PENDING)

						if (cursor.moveToFirst()) {
							val isPending = cursor.getIntOrNull(isPendingColumn)
							// its already updated so no need to delete
							if (isPending == 0) return@withContext false
						}
					}
				// if its pending then delete the file
				val rowsEffected = contentResolver.delete(file, null, null)
				return@withContext rowsEffected == 1
			} catch (e: Exception) {
				e.printStackTrace()
				false
			}
		}
	}
}

private fun Long.toDateTime() = Instant
	.fromEpochMilliseconds(this)
	.toLocalDateTime(TimeZone.currentSystemDefault())