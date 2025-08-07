package com.eva.editor.domain

import com.eva.recordings.domain.models.AudioFileModel

fun interface EditedItemSaver {

	suspend fun saveItem(model: AudioFileModel, fileUri: String)
}