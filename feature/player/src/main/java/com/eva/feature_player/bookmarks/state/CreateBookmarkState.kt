package com.eva.feature_player.bookmarks.state

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.input.TextFieldValue
import com.eva.bookmarks.domain.AudioBookmarkModel

@Stable
data class CreateBookmarkState(
	val textValue: TextFieldValue = TextFieldValue(),
	val showDialog: Boolean = false,
	val bookMarkModel: AudioBookmarkModel? = null,
) {
	val isUpdate: Boolean
		get() = bookMarkModel != null
}