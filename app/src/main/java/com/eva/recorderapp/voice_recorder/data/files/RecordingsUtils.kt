package com.eva.recorderapp.voice_recorder.data.files

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.RequiresApi
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.net.toUri
import com.eva.recorderapp.voice_recorder.domain.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.domain.models.TrashRecordingModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "RECORDING_UTILS_LOGGER"

abstract class RecordingsUtils(private val context: Context) {

	val epochSeconds: Long
		get() = Clock.System.now().epochSeconds

	val contentResolver
		get() = context.contentResolver

	val musicDir: String
		get() = Environment.DIRECTORY_MUSIC + File.separator + context.packageName

	val volumeUri: Uri
		get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
			MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
		else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

	val recordingsProjection: Array<String>
		get() = arrayOf(
			MediaStore.Audio.AudioColumns._ID,
			MediaStore.Audio.AudioColumns.TITLE,
			MediaStore.Audio.AudioColumns.DISPLAY_NAME,
			MediaStore.Audio.AudioColumns.MIME_TYPE,
			MediaStore.Audio.AudioColumns.DURATION,
			MediaStore.Audio.AudioColumns.SIZE,
			MediaStore.Audio.AudioColumns.DATE_MODIFIED,
			MediaStore.Audio.AudioColumns.DATE_ADDED,
		)

	val trashRecoringsProjection: Array<String>
		get() = arrayOf(
			MediaStore.Audio.AudioColumns._ID,
			MediaStore.Audio.AudioColumns.TITLE,
			MediaStore.Audio.AudioColumns.DISPLAY_NAME,
			MediaStore.Audio.AudioColumns.MIME_TYPE,
			MediaStore.Audio.AudioColumns.DATE_ADDED,
			MediaStore.Audio.AudioColumns.IS_TRASHED,
			MediaStore.Audio.AudioColumns.DATE_EXPIRES,
		)


	fun readNormalRecordingsFromCursor(cursor: Cursor): List<RecordedVoiceModel> {
		val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns._ID)
		val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE)
		val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DISPLAY_NAME)
		val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.MIME_TYPE)
		val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION)
		val sizeColum = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.SIZE)
		val updateColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATE_MODIFIED)
		val createdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATE_ADDED)

		return buildList<RecordedVoiceModel> {

			while (cursor.moveToNext()) {

				val id = cursor.getLong(idColumn)
				val title = cursor.getString(titleColumn)
				val displayName = cursor.getString(nameColumn)
				val duration = cursor.getLong(durationColumn)
				val size = cursor.getLong(sizeColum)
				val dateUpdated = cursor.getInt(updateColumn)
				val dateAdded = cursor.getInt(createdColumn)
				val mimeType = cursor.getString(mimeTypeColumn)
				val uriString = ContentUris.withAppendedId(volumeUri, id).toString()

				val model = RecordedVoiceModel(
					id = id,
					title = title,
					displayName = displayName,
					duration = duration.milliseconds,
					sizeInBytes = size,
					modifiedAt = dateUpdated.toDateTime(),
					recordedAt = dateAdded.toDateTime(),
					fileUri = uriString,
					mimeType = mimeType,
				)
				add(model)
			}
		}
	}

	fun readTrashedRecordingsFromCursor(cursor: Cursor): List<TrashRecordingModel> {
		val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns._ID)
		val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE)
		val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DISPLAY_NAME)
		val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.MIME_TYPE)
		val createdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATE_ADDED)
		val expiresColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATE_EXPIRES)

		return buildList<TrashRecordingModel> {

			while (cursor.moveToNext()) {

				val id = cursor.getLong(idColumn)
				val title = cursor.getString(titleColumn)
				val displayName = cursor.getString(nameColumn)
				val dateAdded = cursor.getInt(createdColumn)
				val mimeType = cursor.getString(mimeTypeColumn)
				val expires = cursor.getInt(expiresColumn)
				val uriString = ContentUris.withAppendedId(volumeUri, id).toString()


				val model = TrashRecordingModel(
					id = id,
					title = title,
					displayName = displayName,
					recordedAt = dateAdded.toDateTime(),
					fileUri = uriString,
					mimeType = mimeType,
					expiresAt = expires.toDateTime()
				)
				add(model)
			}
		}
	}

	/**
	 * Checks if the uri is trashed or not pending
	 * @param uri [Uri] to check for
	 * @param checkPending Set it to true if pending status to be returned
	 * @return [Boolean] indicating if its trashed ,if [checkPending] is true
	 * then it returns if its trashed and not pending
	 */
	suspend fun checkIfUriAlreadyTrashedAndNotPending(
		uri: Uri,
		checkPending: Boolean = false
	): Boolean {
		val projection = arrayOf(
			MediaStore.Audio.AudioColumns.IS_TRASHED,
			MediaStore.Audio.AudioColumns.IS_PENDING
		)
		return contentResolver.query(uri, projection, Bundle(), null)
			?.use { cursor ->
				val trashColumn =
					cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.IS_TRASHED)
				val pendingColumn =
					cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.IS_PENDING)

				if (!cursor.moveToFirst()) return false

				val isTrashed = cursor.getIntOrNull(trashColumn)
				val isPending = cursor.getIntOrNull(pendingColumn)
				// its already updated so no need to delete
				return if (checkPending) isTrashed == 1 && isPending == 0 else isTrashed == 1

			} ?: false
	}

	/**
	 * Checks if a file exits for the given Uri and returns the [MediaStore.Audio.AudioColumns._ID]
	 * column for the uri
	 * @param uri [Uri] to be trashed
	 * @return Media Item Id
	 */
	suspend fun evaluateFileExitsAndReturnId(uri: Uri): Long? {
		val projection = arrayOf(
			MediaStore.Audio.AudioColumns._ID,
			MediaStore.Audio.AudioColumns.DATA
		)

		return contentResolver.query(uri, projection, Bundle(), null)?.use { cursor ->
			val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns._ID)
			val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATA)

			if (!cursor.moveToFirst()) return null

			val id = cursor.getLongOrNull(idColumn)
			val data = cursor.getString(dataColumn)

			val file = File(data)

			if (file.exists()) return id
			return null
		}
	}

	/**
	 * Moves the uri to trash, trash items will be automatically deleted after a certain period of
	 * time
	 * @param uri [Uri] to be trashed
	 * @return [Boolean] indicating if the operation took place properly
	 */
	@RequiresApi(Build.VERSION_CODES.R)
	suspend fun moveUriToTrash(uri: Uri): Boolean {
		val fileID = evaluateFileExitsAndReturnId(uri)
		if (fileID == null) {
			Log.d(TAG, "FILE_ID NOT FOUND")
			return false
		}
		val isTrashed = checkIfUriAlreadyTrashedAndNotPending(uri)
		if (isTrashed) {
			Log.d(TAG, "FILE IS EITHER TRASHED OR PENDING")
			return false
		}

		val updatedMetaData = ContentValues().apply {
			put(MediaStore.Audio.AudioColumns.IS_TRASHED, 1)
			put(MediaStore.Audio.AudioColumns.DATE_MODIFIED, epochSeconds)
		}

		val selection = "${MediaStore.Audio.AudioColumns._ID} = ?"
		val selectionArgs = arrayOf("$fileID")

		contentResolver.update(volumeUri, updatedMetaData, selection, selectionArgs)
		return true

	}

	/**
	 * Removes the uri from the trash and restore the original file
	 * @param uri [Uri] to be restored
	 * @return [Boolean] indicating the update took place without any problems
	 */
	suspend fun removeUriFromTrash(uri: Uri): Boolean {
		return withContext(Dispatchers.IO) {

			val isTrashed = checkIfUriAlreadyTrashedAndNotPending(uri)
			if (!isTrashed) return@withContext false

			val updatedMetaData = ContentValues().apply {
				put(MediaStore.Audio.AudioColumns.IS_TRASHED, 0)
				put(MediaStore.Audio.AudioColumns.DATE_MODIFIED, epochSeconds)
			}

			val rowsUpdated = contentResolver.update(uri, updatedMetaData, null, null)
			rowsUpdated == 1
			// single row affected i.e, the updated file
		}
	}

	/**
	 * Permanently delete the uri from the scoped storage
	 * @param uri The uri to be deleted
	 * @return [Boolean] indicating only a single row of changes made
	 * As uri's are unique there should be only a single change
	 */
	suspend fun permanentDeleteFromStorage(uri: Uri): Boolean {
		// TODO: Let the user know that its a permanent delete
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
		val args = Bundle()
		return contentResolver.query(uri, projection, args, null)?.use { cursor ->
			val column = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.IS_PENDING)

			if (!cursor.moveToFirst()) return false

			val isPending = cursor.getIntOrNull(column)
			// its already updated so no need to delete
			Log.d(TAG, "URI $uri : IS PENDING : $isPending")
			return isPending == 1

		} ?: false
	}

	/**
	 * Converts the [Long] to [LocalDateTime] instance
	 */
	internal fun Int.toDateTime() = Instant.fromEpochSeconds(this.toLong(), 0)
		.toLocalDateTime(TimeZone.currentSystemDefault())

	/**
	 * Converts the [LocalDateTime] to epoch milliseconds
	 */
	internal fun LocalDateTime.toMilliSeconds() =
		toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()


	companion object {

		@RequiresApi(Build.VERSION_CODES.R)
		fun createTrashRequest(
			context: Context,
			models: List<RecordedVoiceModel>
		): IntentSenderRequest {

			val uris = models.map(RecordedVoiceModel::fileUri).map(String::toUri)
			val pendingIntent = MediaStore.createTrashRequest(context.contentResolver, uris, true)

			return IntentSenderRequest.Builder(pendingIntent).build()
		}

		@JvmName("create_delete_requests_from_trash_models")
		@RequiresApi(Build.VERSION_CODES.R)
		fun createDeleteRequest(
			context: Context,
			models: List<TrashRecordingModel>
		): IntentSenderRequest {
			val uris = models.map(TrashRecordingModel::fileUri).map(String::toUri)
			val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)

			return IntentSenderRequest.Builder(pendingIntent).build()
		}

		@JvmName("create_delete_requests_from_recorded_models")
		@RequiresApi(Build.VERSION_CODES.R)
		fun createDeleteRequest(
			context: Context,
			models: List<RecordedVoiceModel>
		): IntentSenderRequest {
			val uris = models.map(RecordedVoiceModel::fileUri).map(String::toUri)
			val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)

			return IntentSenderRequest.Builder(pendingIntent).build()
		}
	}
}