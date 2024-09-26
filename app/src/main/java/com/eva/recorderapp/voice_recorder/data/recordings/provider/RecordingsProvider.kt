package com.eva.recorderapp.voice_recorder.data.recordings.provider

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
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
import androidx.core.os.bundleOf
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.data.util.toLocalDateTime
import com.eva.recorderapp.voice_recorder.domain.player.model.AudioFileModel
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.domain.recordings.models.TrashRecordingModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val TAG = "RECORDING_UTILS_LOGGER"

sealed class RecordingsProvider(private val context: Context) {

	val epochSeconds: Long
		get() = Clock.System.now().epochSeconds

	val contentResolver: ContentResolver
		get() = context.contentResolver

	val recordingsMusicDirPath: String
		get() = Environment.DIRECTORY_MUSIC + File.separator + context.getString(R.string.app_name)

	val volumeUri: Uri
		get() = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)


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
			MediaStore.Audio.AudioColumns.DATA
		)

	protected val trashRecordingsProjection: Array<String>
		get() = arrayOf(
			MediaStore.Audio.AudioColumns._ID,
			MediaStore.Audio.AudioColumns.TITLE,
			MediaStore.Audio.AudioColumns.DISPLAY_NAME,
			MediaStore.Audio.AudioColumns.MIME_TYPE,
			MediaStore.Audio.AudioColumns.DATE_ADDED,
			MediaStore.Audio.AudioColumns.IS_TRASHED,
			MediaStore.Audio.AudioColumns.DATE_EXPIRES,
		)

	protected val detailedFileProjection: Array<String>
		get() = arrayOf(
			MediaStore.Audio.AudioColumns._ID,
			MediaStore.Audio.AudioColumns.TITLE,
			MediaStore.Audio.AudioColumns.DISPLAY_NAME,
			MediaStore.Audio.AudioColumns.SIZE,
			MediaStore.Audio.AudioColumns.DURATION,
			MediaStore.Audio.AudioColumns.DATE_MODIFIED,
			MediaStore.Audio.AudioColumns.DATA,
			MediaStore.Audio.AudioColumns.MIME_TYPE,
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
				val uriString = ContentUris.withAppendedId(volumeUri, id).toString()
				val data = cursor.getString(dataColumn)

				// checking the file exists or not
				// in API-29 we can delete the file without deleting the metadata
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

		return buildList {

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
					recordedAt = dateAdded.seconds.toLocalDateTime(),
					fileUri = uriString,
					mimeType = mimeType,
					expiresAt = expires.seconds.toLocalDateTime()
				)
				add(model)
			}
		}
	}

	suspend fun evaluateValuesFromCursor(cursor: Cursor): AudioFileModel? {
		return coroutineScope {
			val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns._ID)
			val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE)
			val nameColumn =
				cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DISPLAY_NAME)
			val durationColumn =
				cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION)
			val sizeColum = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.SIZE)
			val dateModifiedCol =
				cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATE_MODIFIED)
			val pathColumn =
				cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATA)
			val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.MIME_TYPE)

			if (!cursor.moveToFirst()) return@coroutineScope null

			val id = cursor.getLong(idColumn)
			val title = cursor.getString(titleColumn)
			val displayName = cursor.getString(nameColumn)
			val duration = cursor.getLong(durationColumn)
			val size = cursor.getLong(sizeColum)
			val lastModified = cursor.getInt(dateModifiedCol)
			val relPath = cursor.getString(pathColumn)
			val mimeType = cursor.getString(mimeCol)
			val uriString = ContentUris.withAppendedId(volumeUri, id)
				.toString()

			val extractor = extractMediaInfo(uri = ContentUris.withAppendedId(volumeUri, id))

			AudioFileModel(
				id = id,
				title = title,
				displayName = displayName,
				duration = duration.milliseconds,
				size = size,
				fileUri = uriString,
				bitRateInKbps = extractor?.bitRate ?: 0f,
				lastModified = lastModified.seconds.toLocalDateTime(),
				channel = extractor?.channelCount ?: 0,
				path = relPath,
				mimeType = mimeType,
				samplingRateKHz = (extractor?.sampleRate ?: 0) / 1000f
			)
		}
	}

	/**
	 * Checks if the uri is trashed or not pending
	 * @param uri [Uri] to check for
	 * @return [Pair] of [Boolean] first indicating isTrashed and second one isPending
	 */
	suspend fun checkIfUriAlreadyTrashedAndNotPending(uri: Uri): Pair<Boolean, Boolean> {
		val projection = arrayOf(
			MediaStore.Audio.AudioColumns.IS_TRASHED,
			MediaStore.Audio.AudioColumns.IS_PENDING
		)
		return withContext(Dispatchers.IO) {
			contentResolver.query(uri, projection, Bundle(), null)?.use { cursor ->
				val trashColumn =
					cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.IS_TRASHED)
				val pendingColumn =
					cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.IS_PENDING)

				if (!cursor.moveToFirst()) return@withContext false to false

				val isTrashed = cursor.getIntOrNull(trashColumn) == 1
				val isPending = cursor.getIntOrNull(pendingColumn) == 1
				// its already updated so no need to delete
				return@withContext (isTrashed to isPending)
			} ?: (false to false)
		}
	}

	/**
	 * Checks if a file exits for the given Uri and returns the [MediaStore.Audio.AudioColumns._ID]
	 * column for the uri
	 * @param uri [Uri] to be trashed
	 * @return Media Item I'd
	 */
	private suspend fun evaluateFileExitsAndReturnId(uri: Uri): Long? {
		val projection = arrayOf(
			MediaStore.Audio.AudioColumns._ID,
			MediaStore.Audio.AudioColumns.DATA
		)

		return withContext(Dispatchers.IO) {
			contentResolver.query(uri, projection, bundleOf(), null)
				?.use { cursor ->
					val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns._ID)
					val dataColumn =
						cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATA)

					if (!cursor.moveToFirst()) return@withContext null

					val id = cursor.getLongOrNull(idColumn)
					val data = cursor.getString(dataColumn)

					val file = File(data)

					if (file.exists()) return@withContext id
					null
				}
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
			Log.d(TAG, "FILE IS MISSING")
			return false
		}
		val (isTrashed, _) = checkIfUriAlreadyTrashedAndNotPending(uri)
		if (isTrashed) {
			Log.d(TAG, "FILE IS ALREADY TRASHED")
			return false
		}

		val updatedMetaData = ContentValues().apply {
			put(MediaStore.Audio.AudioColumns.IS_TRASHED, 1)
			put(MediaStore.Audio.AudioColumns.DATE_MODIFIED, epochSeconds)
		}

		val rowsUpdated = contentResolver.update(uri, updatedMetaData, null, null)

		return rowsUpdated == 1

	}

	/**
	 * Removes the uri from the trash and restore the original file
	 * @param uri [Uri] to be restored
	 * @return [Boolean] indicating the update took place without any problems
	 */
	suspend fun removeUriFromTrash(uri: Uri): Boolean {
		return withContext(Dispatchers.IO) {

			val (isTrashed, _) = checkIfUriAlreadyTrashedAndNotPending(uri)
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
		return withContext(Dispatchers.IO) {
			contentResolver.query(uri, projection, args, null)?.use { cursor ->
				val column = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.IS_PENDING)

				if (!cursor.moveToFirst()) return@withContext false

				val isPending = cursor.getIntOrNull(column)
				// its already updated so no need to delete
				Log.d(TAG, "URI $uri : IS PENDING : $isPending")
				return@withContext isPending == 1
			} ?: false
		}
	}


	private suspend fun extractMediaInfo(uri: Uri): MediaInfo? {
		val extractor = MediaExtractor()
		val retriever = MediaMetadataRetriever()
		try {
			return withContext(Dispatchers.Default) {// set source
				extractor.setDataSource(context, uri, null)
				retriever.setDataSource(context, uri)
				// its accountable that there is a single track
				val mediaFormat = extractor.getTrackFormat(0)
				val channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

				val sampleRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
					retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
						?.toIntOrNull() ?: 0
				} else mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)

				val bitRateInKbps = retriever
					.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
					?.toIntOrNull()?.let { it / 1000f }
					?: 0f

				MediaInfo(
					channelCount = channelCount,
					sampleRate = sampleRate,
					bitRate = bitRateInKbps
				)
			}
		} catch (e: Exception) {
			e.printStackTrace()
			return null
		} finally {
			retriever.release()
			extractor.release()
		}
	}

	protected data class MediaInfo(
		val channelCount: Int = 0,
		val sampleRate: Int = 0,
		val bitRate: Float = 0f,
	)


	companion object {

		@RequiresApi(Build.VERSION_CODES.R)
		fun createTrashRequest(
			context: Context, models: Collection<RecordedVoiceModel>
		): IntentSenderRequest {

			val uris = models.map(RecordedVoiceModel::fileUri).map(String::toUri)
			val pendingIntent = MediaStore.createTrashRequest(context.contentResolver, uris, true)

			return IntentSenderRequest.Builder(pendingIntent).build()
		}

		@JvmName("create_delete_requests_from_trash_models")
		@RequiresApi(Build.VERSION_CODES.R)
		fun createDeleteRequest(
			context: Context, models: Collection<TrashRecordingModel>
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