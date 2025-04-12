package com.eva.recordings.data.wrapper

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import androidx.core.os.bundleOf
import com.eva.recordings.domain.models.RecordedVoiceModel
import com.eva.recordings.domain.models.TrashRecordingModel
import com.eva.utils.toLocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val TAG = "RECORDINGS_CONTENT_RESOLVER_WRAPPER"

internal abstract class RecordingsContentResolverWrapper(private val context: Context) {

	val epochSeconds: Long
		get() = Clock.System.now().epochSeconds

	val contentResolver: ContentResolver
		get() = context.contentResolver


	protected val recordingsProjection: Array<String>
		get() = arrayOf(
			MediaStore.Audio.AudioColumns._ID,
			MediaStore.Audio.AudioColumns.TITLE,
			MediaStore.Audio.AudioColumns.DISPLAY_NAME,
			MediaStore.Audio.AudioColumns.MIME_TYPE,
			MediaStore.Audio.AudioColumns.DURATION,
			MediaStore.Audio.AudioColumns.SIZE,
			MediaStore.Audio.AudioColumns.DATE_MODIFIED,
			MediaStore.Audio.AudioColumns.DATE_ADDED,
			MediaStore.Audio.AudioColumns.DATA,
			MediaStore.Audio.AudioColumns.OWNER_PACKAGE_NAME,
		)

	val trashRecordingsProjection: Array<String>
		get() = arrayOf(
			MediaStore.Audio.AudioColumns._ID,
			MediaStore.Audio.AudioColumns.TITLE,
			MediaStore.Audio.AudioColumns.DISPLAY_NAME,
			MediaStore.Audio.AudioColumns.MIME_TYPE,
			MediaStore.Audio.AudioColumns.DATE_ADDED,
			MediaStore.Audio.AudioColumns.IS_TRASHED,
			MediaStore.Audio.AudioColumns.DATE_EXPIRES,
			MediaStore.Audio.AudioColumns.OWNER_PACKAGE_NAME,
		)

	protected fun readNormalRecordingsFromCursor(cursor: Cursor): List<RecordedVoiceModel> {
		val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns._ID)
		val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE)
		val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DISPLAY_NAME)
		val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.MIME_TYPE)
		val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION)
		val sizeColum = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.SIZE)
		val updateColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATE_MODIFIED)
		val createdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATE_ADDED)
		val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATA)
		val packageNameColumn =
			cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.OWNER_PACKAGE_NAME)

		return buildList {

			while (cursor.moveToNext()) {

				val id = cursor.getLong(idColumn)
				val title = cursor.getString(titleColumn)
				val displayName = cursor.getString(nameColumn)
				val duration = cursor.getLong(durationColumn)
				val size = cursor.getLong(sizeColum)
				val dateUpdated = cursor.getInt(updateColumn)
				val dateAdded = cursor.getInt(createdColumn)
				val mimeType = cursor.getString(mimeTypeColumn)
				val uriString = ContentUris.withAppendedId(RecordingsConstants.AUDIO_VOLUME_URI, id).toString()
				val data = cursor.getString(dataColumn)
				val packageName = cursor.getStringOrNull(packageNameColumn)

				// checking the file exists or not
				// in API-29 we can delete the file without deleting the mediator entry
				if (!File(data).exists()) continue

				val model = RecordedVoiceModel(
					id = id,
					title = title,
					displayName = displayName,
					duration = duration.milliseconds,
					sizeInBytes = size,
					modifiedAt = dateUpdated.seconds.toLocalDateTime(),
					recordedAt = dateAdded.seconds.toLocalDateTime(),
					fileUri = uriString,
					mimeType = mimeType,
					owner = packageName
				)
				add(model)
			}
		}
	}

	protected fun readTrashedRecordingsFromCursor(cursor: Cursor): List<TrashRecordingModel> {
		val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns._ID)
		val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE)
		val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DISPLAY_NAME)
		val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.MIME_TYPE)
		val createdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATE_ADDED)
		val expiresColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATE_EXPIRES)
		val ownerColumn =
			cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.OWNER_PACKAGE_NAME)

		return buildList {

			while (cursor.moveToNext()) {

				val id = cursor.getLong(idColumn)
				val title = cursor.getString(titleColumn)
				val displayName = cursor.getString(nameColumn)
				val dateAdded = cursor.getInt(createdColumn)
				val mimeType = cursor.getString(mimeTypeColumn)
				val expires = cursor.getInt(expiresColumn)
				val owner = cursor.getStringOrNull(ownerColumn)
				val uriString = ContentUris.withAppendedId(RecordingsConstants.AUDIO_VOLUME_URI, id).toString()


				val model = TrashRecordingModel(
					id = id,
					title = title,
					displayName = displayName,
					recordedAt = dateAdded.seconds.toLocalDateTime(),
					fileUri = uriString,
					mimeType = mimeType,
					owner = owner,
					expiresAt = expires.seconds.toLocalDateTime(),
				)
				add(model)
			}
		}
	}

	/**
	 * Checks if the uri is trashed or not pending
	 * @param uri [Uri] to check for
	 * @return [Pair] of [Boolean] first indicating isTrashed and second one isPending
	 */
	private suspend fun checkIfUriTrashed(uri: Uri): Boolean {
		val projection = arrayOf(
			MediaStore.Audio.AudioColumns.IS_TRASHED,
		)
		return withContext(Dispatchers.IO) {
			contentResolver.query(uri, projection, Bundle(), null)?.use { cursor ->
				val trashColumn =
					cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.IS_TRASHED)

				if (!cursor.moveToFirst()) return@withContext false
				cursor.getIntOrNull(trashColumn) == 1
			} == true
		}
	}

	suspend fun moveUrisToOrFromTrash(recordingsUris: Collection<Uri>, fromTrash: Boolean = true) {
		return coroutineScope {
			val trashedRecordings = recordingsUris.filter { uri ->
				val isTrashed = checkIfUriTrashed(uri)
				// if both of then true or false then it's true
				(isTrashed xor fromTrash).not()
			}

			if (trashedRecordings.isEmpty()) {
				Log.d(TAG, "THERE ARE NO RECORDINGS TO INTERFERE WITH")
				return@coroutineScope
			}

			val trashFlag = if (fromTrash) 0 else 1

			val metadata = ContentValues().apply {
				put(MediaStore.Audio.AudioColumns.IS_TRASHED, trashFlag)
			}
			// don't put uris with non owner from this app
			withContext(Dispatchers.IO) {
				supervisorScope {
					val results = trashedRecordings.map { uri ->
						async {
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
								contentResolver.update(uri, metadata, null)
							else contentResolver.update(uri, metadata, null, null)
						}
					}
					results.awaitAll()
				}
			}
		}
	}


	suspend fun permanentDeleteUrisFromAudioMediaVolume(recordingsUris: Collection<Uri>) {
		// don't put uris with non owner from this app
		// this may lead to security exception and exceptions are not handled
		withContext(Dispatchers.IO) {
			supervisorScope {
				val results = recordingsUris.map { uri ->
					async {
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
							contentResolver.delete(uri, null)
						else contentResolver.delete(uri, null, null)
					}
				}
				results.awaitAll()
			}
		}
	}

	/**
	 * Permanently delete the uri from the scoped storage
	 * @param uri The uri to be deleted
	 * @return [Boolean] indicating only a single row of changes made
	 * As uri's are unique there should be only a single change
	 */
	suspend fun permanentDeleteFromStorage(uri: Uri): Boolean {
		return withContext(Dispatchers.IO) {
			val rowsChanged = contentResolver.delete(uri, null, null)
			rowsChanged == 1
		}
	}

	/**
	 * Checks if the given uri  [uri] is pending or not
	 * @param uri [Uri] to check
	 * @return [Boolean] indicating if its pending
	 */
	suspend fun checkIfUriIsPending(uri: Uri): Boolean {
		val projection = arrayOf(MediaStore.Audio.AudioColumns.IS_PENDING)

		return withContext(Dispatchers.IO) {
			contentResolver.query(uri, projection, bundleOf(), null)?.use { cursor ->
				val column = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.IS_PENDING)

				if (!cursor.moveToFirst()) return@withContext false
				val isPending = cursor.getIntOrNull(column)

				isPending == 1
			} == true
		}
	}

}