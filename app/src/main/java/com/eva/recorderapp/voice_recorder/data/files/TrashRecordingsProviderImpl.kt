package com.eva.recorderapp.voice_recorder.data.files

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.database.SQLException
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.files.ResourcedVoiceRecordingModels
import com.eva.recorderapp.voice_recorder.domain.files.TrashRecordingsProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val LOGGER_TAG = "TRASHED_RECORINGS_PROVIDER"

class TrashRecordingsProviderImpl(
	private val context: Context
) : RecordingsUtils(context), TrashRecordingsProvider {

	override val trashedRecordingsFlow: Flow<ResourcedVoiceRecordingModels>
		get() = callbackFlow {

			val scope = CoroutineScope(Dispatchers.IO)

			trySend(Resource.Loading)

			scope.launch {
				// for the first time we need the query the current
				val recordings = getTrashedVoiceRecordings()
				send(recordings)
			}

			val observer = object : ContentObserver(null) {
				override fun onChange(selfChange: Boolean, uri: Uri?, flags: Int) {
					super.onChange(selfChange, uri, flags)

					Log.d(LOGGER_TAG, "CONTENT CHANGED")
					// if the content updated then resend the values
					scope.launch {
						val recordings = getTrashedVoiceRecordings()
						send(recordings)
					}
				}
			}

			Log.d(LOGGER_TAG, "ADDED OBSERVER FOR TRAHSED ITEMS")
			contentResolver.registerContentObserver(volumeUri, true, observer)

			awaitClose {
				Log.d(LOGGER_TAG, "CANCELED OBSERVER FOR TRASH ITEMS")
				scope.cancel()
				contentResolver.unregisterContentObserver(observer)
			}
		}

	override suspend fun getTrashedVoiceRecordings(): ResourcedVoiceRecordingModels {

		return withContext(Dispatchers.IO) {
			try {
				val models = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
					val selection = "${MediaStore.Audio.AudioColumns.OWNER_PACKAGE_NAME} = ? "
					val selectionArgs = arrayOf(context.packageName)

					val queryArgs = Bundle().apply {
						//selection
						putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
						putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
						// show only trashed items
						putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
						//sorting
						putStringArray(
							ContentResolver.QUERY_ARG_SORT_COLUMNS,
							arrayOf(MediaStore.Audio.AudioColumns.DATE_ADDED)
						)
						putInt(
							ContentResolver.QUERY_ARG_SORT_DIRECTION,
							ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
						)
					}
					contentResolver.query(volumeUri, baseProjection, queryArgs, null)
						?.use { cursor -> recordingsFromCursor(cursor, volumeUri) }
						?: emptyList()

				} else {
					// TODO: Check for API-29 if its works
					val selection = buildString {
						append(MediaStore.Audio.AudioColumns.OWNER_PACKAGE_NAME)
						append(" = ? ")
						append(" AND ")
						append(MediaStore.Audio.AudioColumns.IS_TRASHED)
						append(" = ? ")
					}
					val selectionArgs = arrayOf(context.packageName, "1")
					contentResolver.query(volumeUri, baseProjection, selection, selectionArgs, null)
						?.use { cursor -> recordingsFromCursor(cursor, volumeUri) }
						?: emptyList()
				}
				Resource.Success(models)
			} catch (e: SQLException) {
				Resource.Error(e, "SQL EXCEPTION")
			} catch (e: Exception) {
				e.printStackTrace()
				Resource.Error(e, e.message)
			}
		}
	}

}