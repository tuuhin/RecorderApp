package com.eva.recorderapp.voice_recorder.data.recordings.provider

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.database.SQLException
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.player.PlayerFileProvider
import com.eva.recorderapp.voice_recorder.domain.player.ResourcedDetailedRecordingModel
import com.eva.recorderapp.voice_recorder.domain.player.exceptions.PlayerFileNotFoundException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "PLAYER_FILE_PROVIDER"

class PlayerFileProviderImpl(context: Context) : RecordingsProvider(context), PlayerFileProvider {

	override fun providesAudioFileUri(audioId: Long): Uri {
		return ContentUris.withAppendedId(volumeUri, audioId)
	}

	override fun getAudioFileInfo(id: Long): Flow<ResourcedDetailedRecordingModel> {
		return callbackFlow {

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

			val fileContentUri = ContentUris.withAppendedId(volumeUri, id)
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
				val result = contentResolver
					.query(volumeUri, detailedFileProjection, bundle, null)
					?.use { cur -> evaluateValuesFromCursor(cur) }

				result?.let { Resource.Success(it) }
					?: Resource.Error(PlayerFileNotFoundException())
			} catch (e: SecurityException) {
				// the uri has a different owner, and we don't have proper permission to handle
				// this case
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
}