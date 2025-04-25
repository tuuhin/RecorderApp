package com.eva.feature_player.bookmarks.state

import androidx.compose.ui.text.input.TextFieldValue
import com.eva.bookmarks.domain.AudioBookmarkModel
import kotlin.time.Duration

internal sealed interface BookMarkEvents {

	data class OnDeleteBookmark(val bookmarkModel: AudioBookmarkModel) : BookMarkEvents
	data class OnUpdateTextField(val textFieldValue: TextFieldValue) : BookMarkEvents

	data class OpenDialogToEdit(val bookMark: AudioBookmarkModel) : BookMarkEvents
	data object OnCloseDialog : BookMarkEvents
	data object OpenDialogToCreate : BookMarkEvents

	data class OnAddOrUpdateBookMark(val time: Duration) : BookMarkEvents

	data object OnExportBookMarkPoints : BookMarkEvents
}