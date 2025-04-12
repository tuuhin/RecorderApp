package com.eva.recordings.data.wrapper

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.eva.recordings.domain.models.RecordedVoiceModel
import com.eva.recordings.domain.models.TrashRecordingModel

private const val TAG = "INTENT_MEDIA_REQUESTER"

object RecordingsMediaRequester {

	@RequiresApi(Build.VERSION_CODES.R)
	fun createTrashRequest(context: Context, models: Collection<RecordedVoiceModel>)
			: IntentSenderRequest? {

		val uris = models.filterNot { it.owner == context.packageName }
			.map(RecordedVoiceModel::fileUri)
			.map(String::toUri)

		if (uris.isEmpty()) {
			Log.d(TAG, "NO URIS CANNOT BE DELETED")
			return null
		}

		Log.d(TAG, "NO. OF URI TO TRASH ${uris.size}")
		val pendingIntent = MediaStore.createTrashRequest(context.contentResolver, uris, true)

		return IntentSenderRequest.Builder(pendingIntent)
			.build()
	}

	@JvmName("create_delete_requests_from_trash_models")
	@RequiresApi(Build.VERSION_CODES.R)
	fun createDeleteRequest(context: Context, models: Collection<TrashRecordingModel>)
			: IntentSenderRequest? {

		val uris = models.filterNot { it.owner == context.packageName }
			.map(TrashRecordingModel::fileUri)
			.map(String::toUri)

		if (uris.isEmpty()) {
			Log.d(TAG, "NO URIS CANNOT BE DELETED")
			return null
		}

		val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)

		return IntentSenderRequest.Builder(pendingIntent).build()
	}

	@RequiresApi(Build.VERSION_CODES.R)
	fun createWriteRequest(context: Context, recording: RecordedVoiceModel)
			: IntentSenderRequest {
		val uris = recording.fileUri.toUri()

		val pendingIntent = MediaStore.createWriteRequest(context.contentResolver, listOf(uris))

		return IntentSenderRequest.Builder(pendingIntent).build()
	}

	@JvmName("create_delete_requests_from_recorded_models")
	@RequiresApi(Build.VERSION_CODES.R)
	fun createDeleteRequest(context: Context, models: List<RecordedVoiceModel>)
			: IntentSenderRequest? {

		val uris = models.map(RecordedVoiceModel::fileUri).map(String::toUri)

		if (uris.isEmpty()) {
			Log.d(TAG, "NO URIS PROVIDED")
			return null
		}

		val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)

		return IntentSenderRequest.Builder(pendingIntent).build()
	}
}