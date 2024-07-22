package com.eva.recorderapp.voice_recorder.data.files

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.eva.recorderapp.voice_recorder.domain.models.RecordedVoiceModel
import java.io.File

abstract class RecordingsUtils(private val context: Context) {

	val contentResolver
		get() = context.contentResolver

	val musicDir: String
		get() = Environment.DIRECTORY_MUSIC + File.separator + context.packageName

	val volumeUri: Uri
		get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
			MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
		else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

	val baseProjection: Array<String>
		get() = arrayOf(
			MediaStore.Audio.AudioColumns._ID,
			MediaStore.Audio.AudioColumns.TITLE,
			MediaStore.Audio.AudioColumns.DISPLAY_NAME,
			MediaStore.Audio.AudioColumns.DURATION,
			MediaStore.Audio.AudioColumns.SIZE,
			MediaStore.Audio.AudioColumns.DATE_MODIFIED,
			MediaStore.Audio.AudioColumns.DATE_ADDED,
			MediaStore.Audio.AudioColumns.IS_TRASHED,
			MediaStore.Audio.AudioColumns.DATE_EXPIRES
		)

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

