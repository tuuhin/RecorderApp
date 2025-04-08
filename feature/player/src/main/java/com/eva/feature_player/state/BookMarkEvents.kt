package com.eva.feature_player.state

import androidx.compose.ui.text.input.TextFieldValue
import com.eva.bookmarks.domain.AudioBookmarkModel
import kotlinx.datetime.LocalTime

internal sealed interface BookMarkEvents {

	data class OnDeleteBookmark(val bookmarkModel: AudioBookmarkModel) : BookMarkEvents
	data class OnUpdateTextField(val textFieldValue: TextFieldValue) : BookMarkEvents

	data class OpenDialogToEdit(val bookMark: AudioBookmarkModel) : BookMarkEvents
	data object OnCloseDialog : BookMarkEvents
	data object OpenDialogToCreate : BookMarkEvents

	data class OnAddOrUpdateBookMark(val time: LocalTime) : BookMarkEvents

	data object OnExportBookMarkPoints : BookMarkEvents
}