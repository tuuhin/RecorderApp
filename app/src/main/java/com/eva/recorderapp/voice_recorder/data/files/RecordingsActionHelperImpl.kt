package com.eva.recorderapp.voice_recorder.data.files

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.eva.recorderapp.R
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.files.RecordingsActionHelper
import com.eva.recorderapp.voice_recorder.domain.models.RecordedVoiceModel

class RecordingsActionHelperImpl(
	private val context: Context
) : RecordingsActionHelper {

	override fun shareAudioFiles(collection: List<RecordedVoiceModel>): Resource<Unit, Exception> {

		val extras = arrayListOf<Uri>()
		val uris = collection.map { item -> item.fileUri.toUri() }
		extras.addAll(uris)
		val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
			type = "audio/*"
			putExtra(Intent.EXTRA_SUBJECT, "Sending recorded audio files")
			putParcelableArrayListExtra(Intent.EXTRA_STREAM, extras)
		}

		val intentChooser = Intent.createChooser(intent, context.getString(R.string.share_intent_choose_title))
			.apply {
				flags = Intent.FLAG_ACTIVITY_NEW_TASK
			}

		return try {
			context.startActivity(intentChooser)
			Resource.Success(Unit)
		} catch (e: ActivityNotFoundException) {
			Resource.Error(e, "No Activity found to share content to")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e)
		}

	}

}