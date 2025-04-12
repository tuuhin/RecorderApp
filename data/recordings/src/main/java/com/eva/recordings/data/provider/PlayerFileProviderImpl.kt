package com.eva.recordings.data.provider

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
import android.provider.MediaStore
import android.util.Log
import androidx.core.os.bundleOf
import com.eva.location.domain.repository.LocationAddressProvider
import com.eva.location.domain.utils.parseLocationFromString
import com.eva.recordings.data.utils.MediaMetaDataInfo
import com.eva.recordings.data.wrapper.RecordingsConstants
import com.eva.recordings.data.wrapper.RecordingsContentResolverWrapper
import com.eva.recordings.domain.exceptions.InvalidRecordingIdException
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.recordings.domain.provider.PlayerFileProvider
import com.eva.recordings.domain.provider.ResourcedDetailedRecordingModel
import com.eva.utils.Resource
import com.eva.utils.toLocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val TAG = "PLAYER_FILE_PROVIDER"

internal class PlayerFileProviderImpl(
	private val context: Context,
	private val addressProvider: LocationAddressProvider,
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

	override fun providesAudioFileUri(audioId: Long): String {
		return ContentUris.withAppendedId(RecordingsConstants.AUDIO_VOLUME_URI, audioId).toString()
	}

	override fun getAudioFileInfo(id: Long): Flow<ResourcedDetailedRecordingModel> {
		return callbackFlow {

			var updateJob: Job? = null
			// send loading
			trySend(Resource.Loading)

			// send the data
			launch(Dispatchers.IO) {
				// evaluate it and send
				val first = getPlayerInfoFromAudioId(id)
				send(first)
			}

			val contentObserver = object : ContentObserver(null) {
				override fun onChange(selfChange: Boolean) {
					// observer has found some changes
					// cancel the previous job and run new one
					updateJob?.cancel()
					updateJob = launch(Dispatchers.IO) {
						val update = getPlayerInfoFromAudioId(id)
						send(update)
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

	override suspend fun getPlayerInfoFromAudioId(id: Long): ResourcedDetailedRecordingModel {
		val selection = "${MediaStore.Audio.AudioColumns._ID} = ?"
		val selectionArgs = arrayOf("$id")

		val bundle = bundleOf(
			ContentResolver.QUERY_ARG_SQL_SELECTION to selection,
			ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS to selectionArgs
		)
		return withContext(Dispatchers.IO) {
			try {
				contentResolver.query(
					RecordingsConstants.AUDIO_VOLUME_URI,
					_projection,
					bundle,
					null
				)
					?.use { cur -> evaluateValuesFromCursor(cur) }
					?.let { Resource.Success(it) }
					?: Resource.Error(InvalidRecordingIdException())
			} catch (e: SecurityException) {
				Resource.Error(e, "CANNOT ACCESS FILE PERMISSION WAS NOT GRANTED")
			} catch (e: SQLException) {
				e.printStackTrace()
				Resource.Error(e, "SQL EXCEPTION")
			} catch (e: Exception) {
				e.printStackTrace()
				Resource.Error(e)
			}
		}
	}


	private suspend fun extractMediaInfo(uri: Uri): MediaMetaDataInfo? {
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

				val locationAsString = async {
					parseLocationFromString(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION))
						?.let { addressProvider.invoke(it) } ?: ""
				}

				val bitRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
					?.toIntOrNull() ?: 0

				MediaMetaDataInfo(
					channelCount = channelCount,
					sampleRate = sampleRate,
					bitRate = bitRate / 1_000f,
					locationString = locationAsString.await()
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

			val extractor = extractMediaInfo(contentUri)

			AudioFileModel(
				id = id,
				title = title,
				displayName = displayName,
				duration = duration.milliseconds,
				size = size,
				fileUri = contentUri.toString(),
				bitRateInKbps = extractor?.bitRate ?: 0f,
				lastModified = lastModified.seconds.toLocalDateTime(),
				channel = extractor?.channelCount ?: 0,
				path = relPath,
				mimeType = mimeType,
				samplingRateKHz = (extractor?.sampleRate ?: 0) / 1000f,
				metaDataLocation = extractor?.locationString ?: ""
			)
		}
	}
}
