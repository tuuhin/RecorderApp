package com.eva.recorderapp.voice_recorder.data.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.eva.recorderapp.R
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.bookmarks.AudioBookmarkModel
import com.eva.recorderapp.voice_recorder.domain.bookmarks.ExportBookMarkUriProvider
import com.eva.recorderapp.voice_recorder.domain.bookmarks.ExportBookMarksFailedException
import com.eva.recorderapp.voice_recorder.domain.player.model.AudioFileModel
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.domain.util.ShareRecordingsUtil

class ShareRecordingsUtilImpl(
	private val context: Context,
	private val exportBookMarkUriProvider: ExportBookMarkUriProvider,
) : ShareRecordingsUtil {

	override fun shareAudioFiles(collection: List<RecordedVoiceModel>): Resource<Unit, Exception> {

		val extras = arrayListOf<Uri>()
		val uris = collection.map { item -> item.fileUri.toUri() }
		extras.addAll(uris)
		val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
			type = "audio/*"
			putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_audio_extra_subject))
			putParcelableArrayListExtra(Intent.EXTRA_STREAM, extras)
		}

		val intentChooser =
			Intent.createChooser(intent, context.getString(R.string.share_intent_choose_title))
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

	override fun shareAudioFile(audioFileModel: AudioFileModel): Resource<Unit, Exception> {
		val uri = audioFileModel.fileUri.toUri()

		val intent = Intent(Intent.ACTION_SEND).apply {
			setDataAndType(uri, "audio/*")
			putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_audio_extra_subject))
		}

		val intentChooser =
			Intent.createChooser(intent, context.getString(R.string.share_intent_choose_title))
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

	override suspend fun shareBookmarksCsv(bookmarks: Collection<AudioBookmarkModel>): Resource<Unit, Exception> {

		val uri = exportBookMarkUriProvider.invoke(bookmarks.toSet())?.toUri()
			?: return Resource.Error(ExportBookMarksFailedException())

		val intent = Intent(Intent.ACTION_SEND).apply {
			setDataAndType(uri, "text/csv")
			putExtra(Intent.EXTRA_STREAM, uri)
			addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
		}

		val picker = Intent.createChooser(intent, context.getString(R.string.sharing_bookmarks))
			.apply {
				addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			}

		return try {
			context.startActivity(picker)
			Resource.Success(Unit)
		} catch (e: ActivityNotFoundException) {
			Resource.Error(e, "No Activity found to share content to")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e)
		}
	}

}