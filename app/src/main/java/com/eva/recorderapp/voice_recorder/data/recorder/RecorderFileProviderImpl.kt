package com.eva.recorderapp.voice_recorder.data.recorder

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.media3.common.MimeTypes
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.data.files.RecordingsUtils
import com.eva.recorderapp.voice_recorder.domain.recorder.RecorderFileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

private const val LOGGER_TAG = "RECORDER_FILE_PROVIDE"

class RecorderFileProviderImpl(
	private val context: Context
) : RecordingsUtils(context), RecorderFileProvider {


	override suspend fun createUriForRecording(): Uri? {
		// TODO: Allow user to change the audio file name and also format
		val fileName = "AUD_REC_$epochSeconds"

		val metaData = ContentValues().apply {
			put(MediaStore.Audio.AudioColumns.RELATIVE_PATH, musicDir)
			put(MediaStore.Audio.AudioColumns.TITLE, fileName)
			put(MediaStore.Audio.AudioColumns.DISPLAY_NAME, fileName)
			put(MediaStore.Audio.AudioColumns.MIME_TYPE, MimeTypes.AUDIO_AMR_NB)
			put(MediaStore.Audio.AudioColumns.DATE_ADDED, epochSeconds)
			put(MediaStore.Audio.AudioColumns.DATE_MODIFIED, epochSeconds)
			put(MediaStore.Audio.AudioColumns.ARTIST, context.packageName)
			put(MediaStore.Audio.AudioColumns.IS_PENDING, 1)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				put(MediaStore.Audio.AudioColumns.IS_RECORDING, 1)
			}
		}

		return withContext(Dispatchers.IO) {
			try {
				Log.d(LOGGER_TAG, "CREATING FILE")
				val contenUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
					contentResolver.insert(volumeUri, metaData, null)
				} else {
					contentResolver.insert(volumeUri, metaData)
				}
				Log.d(LOGGER_TAG, "URI CREATED , $contenUri")
				return@withContext contenUri
			} catch (e: IllegalArgumentException) {
				Log.e(LOGGER_TAG, "EXTRAS PROVIDED WRONG")
				null
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
					put(MediaStore.Audio.AudioColumns.DATE_MODIFIED, epochSeconds)
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
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
					contentResolver.delete(uri, null)
				else contentResolver.delete(uri, null, null)
				Log.d(LOGGER_TAG, "URI :$uri DELETED")
			} catch (e: SecurityException) {
				Log.e(LOGGER_TAG, "THERE IS A SECURITY PROBLEM", e)
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}

}

