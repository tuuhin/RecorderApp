package com.eva.recordings.data.provider

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import com.eva.recordings.BuildConfig
import com.eva.recordings.data.utils.evaluateWithTimeRead
import com.eva.recordings.data.wrapper.RecordingsConstants
import com.eva.recordings.data.wrapper.RecordingsContentResolverWrapper
import com.eva.recordings.domain.exceptions.InvalidAudioFileIdException
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.recordings.domain.provider.AudioInfoExtractor
import com.eva.recordings.domain.provider.PlayerFileProvider
import com.eva.recordings.domain.provider.ResourcedDetailedRecordingModel
import com.eva.utils.Resource
import com.eva.utils.toLocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val TAG = "PLAYER_FILE_PROVIDER"

internal class PlayerFileProviderImpl(
	context: Context,
	private val extractor: AudioInfoExtractor,
) : RecordingsContentResolverWrapper(context), PlayerFileProvider {

	private val _projection: Array<String>
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

	override suspend fun providesAudioFileUri(audioId: Long): Result<String> {
		return withContext(Dispatchers.IO) {
			try {
				val contentURI =
					ContentUris.withAppendedId(RecordingsConstants.AUDIO_VOLUME_URI, audioId)
				contentResolver.query(contentURI, arrayOf(MediaStore.MediaColumns._ID), null, null)
					?.use { cursor ->
						if (cursor.count > 0) Result.success(contentURI.toString())
						else return@withContext Result.failure(InvalidAudioFileIdException())
					} ?: return@withContext Result.failure(InvalidAudioFileIdException())
			} catch (_: Exception) {
				Result.failure(InvalidAudioFileIdException())
			}
		}
	}

	override fun getAudioFileFromIdFlow(
		id: Long,
		readMetaData: Boolean
	): Flow<ResourcedDetailedRecordingModel> {
		return callbackFlow {
			// send loading
			trySend(Resource.Loading)

			launch(Dispatchers.IO) {
				val result = getAudioFileFromId(id, false)
				result.fold(
					onSuccess = { model ->
						// send the data without metadata
						send(Resource.Success(model))
						// evaluate metadata
						val metaData = evaluateWithTimeRead(
							loggingTag = TAG,
							readTime = BuildConfig.DEBUG
						) {
							if (!readMetaData) return@evaluateWithTimeRead null
							extractor.extractMediaData(model.fileUri)
						}
						val modelWithMetaData = model.copy(metaData = metaData)
						// send data with metadata
						send(Resource.Success(modelWithMetaData))
					},
					onFailure = { err ->
						if (err is Exception) send(Resource.Error(err))
					},
				)
			}

			val contentObserver = object : ContentObserver(null) {
				override fun onChange(selfChange: Boolean) {
					launch(Dispatchers.IO) {
						val updated = getAudioFileFromId(id, readMetaData)
						updated.fold(
							onSuccess = { send(Resource.Success(it)) },
							onFailure = {
								send(Resource.Error(Exception(it)))
							},
						)
					}
				}
			}

			val fileContentUri =
				ContentUris.withAppendedId(RecordingsConstants.AUDIO_VOLUME_URI, id)
			Log.d(TAG, "ADDED CONTENT OBSERVER FOR $fileContentUri")
			contentResolver.registerContentObserver(fileContentUri, false, contentObserver)

			awaitClose {
				Log.d(TAG, "REMOVED CONTENT OBSERVER FOR $fileContentUri")
				contentResolver.unregisterContentObserver(contentObserver)
			}
		}
	}

	override suspend fun getAudioFileFromId(
		id: Long,
		readMetaData: Boolean
	): Result<AudioFileModel> {
		val selection = "${MediaStore.Audio.AudioColumns._ID} = ?"
		val selectionArgs = arrayOf("$id")

		val bundle = Bundle().apply {
			putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
			putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
		}

		return withContext(Dispatchers.IO) {
			runCatching {
				contentResolver.query(
					RecordingsConstants.AUDIO_VOLUME_URI,
					_projection,
					bundle,
					null
				)?.use { cur ->
					val result = evaluateValuesFromCursor(cur)
						?: return@withContext Result.failure(InvalidAudioFileIdException())

					val metaData = evaluateWithTimeRead(
						loggingTag = TAG,
						readTime = BuildConfig.DEBUG
					) {
						if (!readMetaData) return@use result
						extractor.extractMediaData(result.fileUri)
					}
					result.copy(metaData = metaData)
				} ?: return@withContext Result.failure(InvalidAudioFileIdException())
			}
		}
	}


	private suspend fun evaluateValuesFromCursor(cursor: Cursor): AudioFileModel? {
		return withContext(Dispatchers.IO) {

			val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns._ID)
			val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE)
			val nameColumn =
				cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DISPLAY_NAME)
			val durationColumn =
				cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION)
			val sizeColum = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.SIZE)
			val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATE_MODIFIED)
			val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATA)
			val mimeTypeColumn =
				cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.MIME_TYPE)

			if (!cursor.moveToFirst()) return@withContext null

			val id = cursor.getLong(idColumn)
			val title = cursor.getString(titleColumn)
			val displayName = cursor.getString(nameColumn)
			val duration = cursor.getLong(durationColumn)
			val size = cursor.getLong(sizeColum)
			val lastModified = cursor.getInt(dataCol)
			val relPath = cursor.getString(pathColumn)
			val mimeType = cursor.getString(mimeTypeColumn)
			val contentUri = ContentUris.withAppendedId(RecordingsConstants.AUDIO_VOLUME_URI, id)

			AudioFileModel(
				id = id,
				title = title,
				displayName = displayName,
				duration = duration.milliseconds,
				size = size,
				fileUri = contentUri.toString(),
				lastModified = lastModified.seconds.toLocalDateTime(),
				path = relPath,
				mimeType = mimeType,
			)
		}
	}
}
