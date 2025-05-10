package com.eva.editor.data

import android.content.Context
import com.eva.editor.domain.EditedItemSaver
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.worker.SaveEditedMediaWorker

internal class EditedItemSaverImpl(private val context: Context) : EditedItemSaver {

	override fun saveItem(model: AudioFileModel, fileUri: String) {

		//set the new name later

		try {
			SaveEditedMediaWorker.startWorkerAndObserve(
				context = context,
				fileUri = fileUri,
				mimeType = model.mimeType,
				fileName = "Clipped" + model.title
			)
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

}