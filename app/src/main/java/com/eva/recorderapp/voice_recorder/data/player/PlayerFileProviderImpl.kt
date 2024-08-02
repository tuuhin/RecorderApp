package com.eva.recorderapp.voice_recorder.data.player

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.database.SQLException
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.data.util.toLocalDateTime
import com.eva.recorderapp.voice_recorder.domain.models.AudioFileModel
import com.eva.recorderapp.voice_recorder.domain.player.PlayerFileProvider
import com.eva.recorderapp.voice_recorder.domain.player.ResourcedDetailedRecordingModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "PLAYER_FILE_PROVIDER"

class PlayerFileProviderImpl(
	private val context: Context
) : PlayerFileProvider {

	private val contentResolver: ContentResolver
		get() = context.contentResolver

	private val baseVolume = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
		MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
	else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

	private val projecttion: Array<String>
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

	override fun getAudioFileInfo(id: Long): Flow<ResourcedDetailedRecordingModel> {
		return callbackFlow<ResourcedDetailedRecordingModel> {

			val scope = CoroutineScope(Dispatchers.IO)

			trySend(Resource.Loading)

			scope.launch {
				val first = getPlayerInfoFromAudioId(id)
				send(first)
			}

			val contentObserver = object : ContentObserver(null) {
				override fun onChange(selfChange: Boolean) {
					Log.d(TAG, "CONTENT CHANGED")
					scope.launch {
						val update = getPlayerInfoFromAudioId(id)
						send(update)
					}
				}
			}

			val fileContentUri = ContentUris.withAppendedId(baseVolume, id)
			Log.d(TAG, "ADDED CONTENT OBSERVER FOR $fileContentUri")
			contentResolver.registerContentObserver(fileContentUri, false, contentObserver)

			awaitClose {
				Log.d(TAG, "REMOVED CONTENT OBSERVER FOR $fileContentUri")
				contentResolver.unregisterContentObserver(contentObserver)
				scope.cancel()
			}
		}
	}

	override suspend fun getPlayerInfoFromAudioId(id: Long): ResourcedDetailedRecordingModel {
		val selection = "${MediaStore.Audio.AudioColumns._ID} = ?"
		val selectionArgs = arrayOf("$id")

		val bundle = Bundle().apply {
			putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
			putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
		}
		return withContext(Dispatchers.IO) {
			try {
				val result = contentResolver.query(baseVolume, projecttion, bundle, null)
					?.use { cur -> evalutateValuesFromCursor(cur) }

				return@withContext result
					?.let { Resource.Success(it) }
					?: Resource.Error(PlayerFileNotFoundException())
			} catch (e: SecurityException) {
				// the uri has a differnt owner and we don't have proper permission to handle
				// this case
				Resource.Error(e, "FILE SELECTED IS NOT OF THIS PACKAGE")
			} catch (e: SQLException) {
				e.printStackTrace()
				Resource.Error(e, "SQL EXCEPTION")
			} catch (e: Exception) {
				e.printStackTrace()
				Resource.Error(e)
			}
		}
	}

	private suspend fun evalutateValuesFromCursor(cursor: Cursor): AudioFileModel? {
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
			val uriString = ContentUris.withAppendedId(baseVolume, id)
				.toString()

			val extractor = extractMediaInfo(uri = ContentUris.withAppendedId(baseVolume, id))

			AudioFileModel(
				id = id,
				title = title,
				displayName = displayName,
				duration = duration.milliseconds,
				size = size,
				fileUri = uriString,
				bitRateInKbps = extractor?.bitRate?.toFloat() ?: 0f,
				lastModified = lastModified.milliseconds.toLocalDateTime(),
				channel = extractor?.channelCount ?: 0,
				path = relPath,
				mimeType = mimeType,
				samplingRatekHz = (extractor?.sampleRate ?: 0).let { it / 1000f }
			)
		}
	}

	private suspend fun extractMediaInfo(uri: Uri): MediaInfo? {
		return withContext(Dispatchers.Default) {
			val extractor = MediaExtractor()
			val retriever = MediaMetadataRetriever()
			try {
				// set source
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
			} catch (e: Exception) {
				e.printStackTrace()
				null
			} finally {
				retriever.release()
				extractor.release()
			}
		}
	}

	private data class MediaInfo(
		val channelCount: Int = 0,
		val sampleRate: Int = 0,
		val bitRate: Float = 0f,
	)
}
