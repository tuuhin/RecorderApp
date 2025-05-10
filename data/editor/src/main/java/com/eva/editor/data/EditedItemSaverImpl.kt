package com.eva.editor.data

import android.content.Context
import com.eva.datastore.domain.repository.RecorderFileSettingsRepo
import com.eva.editor.domain.EditedItemSaver
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.worker.SaveEditedMediaWorker

internal class EditedItemSaverImpl(
	private val context: Context,
	private val settings: RecorderFileSettingsRepo
) : EditedItemSaver {

	override fun saveItem(model: AudioFileModel, fileUri: String) {

		val prefix = settings.fileSettings.exportItemPrefix

		val fileName = buildString {
			append(prefix)
			append(" ")
			append(model.title)
		}

		try {
			SaveEditedMediaWorker.startWorkerAndObserve(
				context = context,
				fileUri = fileUri,
				mimeType = model.mimeType,
				fileName = fileName
			)
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

}