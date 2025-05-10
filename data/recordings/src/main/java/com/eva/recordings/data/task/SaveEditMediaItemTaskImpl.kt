package com.eva.recordings.data.task

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.eva.database.dao.RecordingsMetadataDao
import com.eva.database.entity.RecordingsMetaDataEntity
import com.eva.recordings.data.wrapper.RecordingsConstants
import com.eva.recordings.domain.tasks.SaveEditMediaItemTask
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

private const val TAG = "SAVE_MEDIA_ITEM_TASK"

internal class SaveEditMediaItemTaskImpl(
	private val context: Context,
	private val recordingDao: RecordingsMetadataDao,
) : SaveEditMediaItemTask {

	private val contentResolver by lazy { context.contentResolver }

	override suspend fun invoke(
		fileName: String,
		mimeType: String,
		operation: suspend (String) -> Unit
	): Result<Unit> {
		return coroutineScope {
			// create the content uri
			val contentUri = createUriForFileName(fileName = fileName, mimeType = mimeType) ?: run {
				Log.d(TAG, "CANNOT CREATE URI FOR RECORDING")
				return@coroutineScope Result.failure(Exception("Cannot create file"))
			}

			try {
				Log.d(TAG, "UPDATING THE FILE CONTENT..")
				val job = launch(Dispatchers.IO) {
					operation(contentUri.toString())
				}
				job.join()
			} catch (e: Exception) {
				Log.d(TAG, "SOME EXCEPTION")
				withContext(NonCancellable) {
					deleteUriIfAnyProblem(contentUri)
				}
				if (e is CancellationException) throw e
			}

			try {
				val uriId = ContentUris.parseId(contentUri)
				// update the metadata for the file
				val mediaStoreUpdate = async { updateUriOnContent(contentUri) }
				// save the secondary metadata
				val otherMetadataDataUpdate = async {
					val entity = RecordingsMetaDataEntity(recordingId = uriId)
					recordingDao.updateOrInsertRecordingMetadata(entity)
				}
				// execute them parallel
				Log.d(TAG, "UPDATING METADATA CONCURRENTLY")
				awaitAll(mediaStoreUpdate, otherMetadataDataUpdate)
				Log.d(TAG, "UPDATE COMPLETED")
				Result.success(Unit)
			} catch (e: CancellationException) {
				Log.d(TAG, "CANCELLED DURING AWAITING OPERATION")
				withContext(NonCancellable) {
					deleteUriIfAnyProblem(contentUri)
				}
				throw e
			} catch (e: IllegalArgumentException) {
				Log.e(TAG, "EXTRAS PROVIDED WRONG", e)
				Result.failure(e)
			} catch (e: IOException) {
				e.printStackTrace()
				Result.failure(e)
			}

		}
	}

	private suspend fun createUriForFileName(fileName: String, mimeType: String): Uri? =
		coroutineScope {
			// metadata
			val metaData = ContentValues().apply {
				put(
					MediaStore.Audio.AudioColumns.RELATIVE_PATH,
					RecordingsConstants.RECORDINGS_MUSIC_PATH
				)
				put(MediaStore.Audio.AudioColumns.DISPLAY_NAME, fileName)
				put(MediaStore.Audio.AudioColumns.MIME_TYPE, mimeType)
				put(MediaStore.Audio.AudioColumns.DATE_ADDED, System.currentTimeMillis())
				put(MediaStore.Audio.AudioColumns.IS_PENDING, 1)
			}

			// insert the metadata on IO thread
			withContext(Dispatchers.IO) {
				Log.d(TAG, "CREATING FILE WITH METADATA :$metaData")
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
					contentResolver.insert(RecordingsConstants.AUDIO_VOLUME_URI, metaData, null)
				} else {
					contentResolver.insert(RecordingsConstants.AUDIO_VOLUME_URI, metaData)
				}
			}
		}

	private suspend fun updateUriOnContent(file: Uri): Boolean {
		val updatedMetaData = ContentValues().apply {
			put(MediaStore.Audio.AudioColumns.IS_PENDING, 0)
			put(MediaStore.Audio.AudioColumns.DATE_MODIFIED, System.currentTimeMillis())
		}
		Log.d(TAG, "UPDATING FILE METADATA :$updatedMetaData")

		val result = withContext(Dispatchers.IO) {
			contentResolver.update(file, updatedMetaData, null, null)
		}
		Log.d(TAG, "UPDATED URI AFTER RECORDING")

		return result == 1
	}

	private suspend fun deleteUriIfAnyProblem(uri: Uri): Boolean {
		Log.d(TAG, "DELETING FILE DUE TO SOME PROBLEM")
		val rowsRemoved = withContext(Dispatchers.IO) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
				contentResolver.delete(uri, null)
			else contentResolver.delete(uri, null, null)
		}
		return rowsRemoved == 1
	}
}