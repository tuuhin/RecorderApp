package com.eva.recorderapp.voice_recorder.data.recorder

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.core.database.getIntOrNull
import androidx.media3.common.MimeTypes
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.data.files.RecordingsUtils
import com.eva.recorderapp.voice_recorder.domain.recorder.RecorderFileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.io.IOException

private const val LOGGER_TAG = "RECORDER_FILE_PROVIDE"

class RecorderFileProviderImpl(
	private val context: Context
) : RecordingsUtils(context), RecorderFileProvider {

	override suspend fun createUriForRecording(): Uri? {
		// TODO: Allow user to change the audio file name and also format
		val time = Clock.System.now().epochSeconds
		val fileName = "AUD_REC_$time"

		val metaData = ContentValues().apply {
			put(MediaStore.Audio.AudioColumns.RELATIVE_PATH, musicDir)
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
				val finalUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
					contentResolver.insert(volumeUri, metaData, null)
				} else {
					contentResolver.insert(volumeUri, metaData)
				}
				Log.d(LOGGER_TAG, "URI CREATED , $finalUri")
				finalUri
			} catch (e: IOException) {
				Log.e(LOGGER_TAG, "IO EXCEPTION", e)
				e.printStackTrace()
				null
			}
		}
	}

	override suspend fun updateUriAfterRecording(file: Uri): Resource<Unit, Exception> {
		return withContext(Dispatchers.IO) {
			try {
				val updatedMetaData = ContentValues().apply {
					put(MediaStore.Audio.AudioColumns.IS_PENDING, 0)
					put(MediaStore.Audio.AudioColumns.DATE_MODIFIED, System.currentTimeMillis())
				}
				contentResolver.update(file, updatedMetaData, null, null)
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
	}

	override suspend fun deleteUriIfNotPending(uri: Uri) {
		return withContext(Dispatchers.IO) {
			try {
				val isPending = checkIfUriIsPending(uri)
				// if its not pending don't do anything
				if (!isPending) return@withContext
				// otherwise delete the pending uri
				contentResolver.delete(uri, null, null)
			} catch (e: SecurityException) {
				Log.e(LOGGER_TAG, "THERE IS A SECURITY PROBLEM", e)
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}

	/**
	 * Checks if the [uri] is pending or not
	 */
	private suspend fun checkIfUriIsPending(uri: Uri): Boolean {
		val projection = arrayOf(MediaStore.Audio.AudioColumns.IS_PENDING)
		val args = Bundle()
		return contentResolver.query(uri, projection, args, null)?.use { cursor ->
			val column = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.IS_PENDING)

			if (cursor.moveToFirst()) {
				val isPending = cursor.getIntOrNull(column)
				// its already updated so no need to delete
				Log.d(LOGGER_TAG, "URI $uri : IS PENDING : $isPending")
				return isPending == 1
			} else false
		} ?: false
	}

}

