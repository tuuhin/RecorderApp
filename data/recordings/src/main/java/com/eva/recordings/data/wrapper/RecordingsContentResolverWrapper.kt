package com.eva.recordings.data.wrapper

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
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
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

private const val TAG = "RECORDINGS_CONTENT_RESOLVER_WRAPPER"

internal abstract class RecordingsContentResolverWrapper(private val context: Context) {

	@OptIn(ExperimentalTime::class)
	val epochSeconds: Long
		get() = Clock.System.now().epochSeconds

	protected val contentResolver: ContentResolver
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
				val uriString = ContentUris.withAppendedId(RecordingsConstants.AUDIO_VOLUME_URI, id)
					.toString()
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
				val uriString = ContentUris.withAppendedId(RecordingsConstants.AUDIO_VOLUME_URI, id)
					.toString()


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
	 * @param uri The [Uri] of the audio file that to be checked
	 * @return [Boolean] Indicating the URI is really trashed, if the uri cannot be found then it's a null
	 */
	private suspend fun checkIfUriTrashed(uri: Uri): Boolean? {
		return withContext(Dispatchers.IO) {
			contentResolver.query(
				uri,
				arrayOf(MediaStore.Audio.AudioColumns.IS_TRASHED),
				bundleOf(),
				null
			)?.use { cursor ->
				val col = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.IS_TRASHED)

				if (!cursor.moveToFirst()) return@withContext false
				cursor.getIntOrNull(col) == 1
			}
		}
	}

	suspend fun moveToTrashOrRestoreFromTrash(recordingsUris: Set<Uri>, fromTrash: Boolean = true) {
		val trashedRecordings = recordingsUris.filter { uri ->
			val isTrashed = checkIfUriTrashed(uri) ?: return@filter false
			// if both of then true or both false
			(isTrashed xor fromTrash).not()
		}

		if (trashedRecordings.isEmpty()) {
			Log.d(TAG, "THERE ARE NO RECORDINGS TO INTERFERE WITH")
			return
		}

		val metadata = ContentValues().apply {
			put(MediaStore.Audio.AudioColumns.IS_TRASHED, if (fromTrash) 0 else 1)
		}

		withContext(Dispatchers.IO) {
			supervisorScope {
				val results = trashedRecordings.map { uri ->
					async {
						try {
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
								contentResolver.update(uri, metadata, null)
							else contentResolver.update(uri, metadata, null, null)
						} catch (e: Exception) {
							if (e is SecurityException) throw e
							e.printStackTrace()
							Log.e(TAG, "SOME GENERAL EXCEPTION", e)
						}
					}
				}
				results.awaitAll()
			}
		}
	}

	/**
	 * Permanently removes the [recordingsUris] from shared storage,
	 * Don't provide uri other than the owner of this package
	 */
	suspend fun permanentlyDeleteURIFromScopedStorage(recordingsUris: Set<Uri>) {
		withContext(Dispatchers.IO) {
			supervisorScope {
				val results = recordingsUris.map { uri ->
					async {
						try {
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
								contentResolver.delete(uri, null)
							else contentResolver.delete(uri, null, null)
						} catch (e: Exception) {
							if (e is SecurityException) throw e
							Log.e(TAG, "SOME GENERAL EXCEPTION", e)
						}
					}
				}
				results.awaitAll()
			}
		}
	}

	/**
	 * Permanently delete the given uri from the scoped storage
	 * @param uri The uri to be deleted
	 */
	suspend fun permanentDeleteFromStorage(uri: Uri): Boolean {
		return withContext(Dispatchers.IO) {
			val rows = contentResolver.delete(uri, null, null)
			rows == 1
		}
	}

	/**
	 * Checks if the given uri  [uri] is pending or not
	 * @param uri [Uri] to check
	 * @return [Boolean] indicating if its pending
	 */
	suspend fun checkURIIsPending(uri: Uri): Boolean {
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