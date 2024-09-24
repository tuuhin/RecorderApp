package com.eva.recorderapp.voice_recorder.presentation.record_player.util

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.input.TextFieldValue
import com.eva.recorderapp.voice_recorder.domain.player.model.AudioBookmarkModel

@Stable
data class CreateOrEditBookMarkState(
	val textValue: TextFieldValue = TextFieldValue(),
	val showDialog: Boolean = false,
	val bookMarkModel: AudioBookmarkModel? = null,
) {
	val isUpdate: Boolean
		get() = bookMarkModel != null
}
