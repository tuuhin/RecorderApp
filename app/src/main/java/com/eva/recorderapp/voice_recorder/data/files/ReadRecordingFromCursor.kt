package com.eva.recorderapp.voice_recorder.data.files

import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.core.database.getLongOrNull
import com.eva.recorderapp.voice_recorder.domain.models.RecordedVoiceModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.milliseconds

fun recordingsFromCursor(cursor: Cursor, volumeUri: Uri): List<RecordedVoiceModel> {

	val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns._ID)
	val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE)
	val sizeColum = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.SIZE)
	val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DISPLAY_NAME)
	val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION)
	val updateColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATE_MODIFIED)
	val createdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATE_ADDED)
	val expiresColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATE_EXPIRES)
	val isTrashedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.IS_TRASHED)

	return buildList<RecordedVoiceModel> {

		while (cursor.moveToNext()) {

			val id = cursor.getLong(idColumn)
			val title = cursor.getString(titleColumn)
			val displayName = cursor.getString(nameColumn)
			val duration = cursor.getLong(durationColumn)
			val size = cursor.getLong(sizeColum)
			val dateUpdated = cursor.getLong(updateColumn)
			val dateAdded = cursor.getLong(createdColumn)
			val isTrashed = cursor.getInt(isTrashedColumn)
			val expiredAt = cursor.getLongOrNull(expiresColumn)
			val uriString = ContentUris.withAppendedId(volumeUri, id).toString()

			val model = RecordedVoiceModel(
				id = id,
				title = title,
				displayName = displayName,
				duration = duration.milliseconds,
				sizeInBytes = size.toInt(),
				modifiedAt = dateUpdated.toDateTime(),
				recordedAt = dateAdded.toDateTime(),
				fileUri = uriString,
				isTrashed = isTrashed == 1,
				expiresAt = expiredAt?.toDateTime()
			)
			add(model)
		}
	}
}

private fun Long.toDateTime() =
	Instant.fromEpochMilliseconds(this * 1_000)
		.toLocalDateTime(TimeZone.currentSystemDefault())