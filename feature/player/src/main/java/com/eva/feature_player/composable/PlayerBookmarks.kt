package com.eva.feature_player.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.eva.bookmarks.domain.AudioBookmarkModel
import com.eva.feature_player.bookmarks.composable.AddBookmarkDialogContent
import com.eva.feature_player.bookmarks.composable.AudioBookmarksList
import com.eva.feature_player.bookmarks.state.BookMarkEvents
import com.eva.feature_player.bookmarks.state.CreateBookmarkState
import com.eva.player.domain.model.PlayerTrackData
import com.eva.ui.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerBookMarks(
	trackCurrentTime: () -> LocalTime,
	bookmarks: ImmutableList<AudioBookmarkModel>,
	bookMarkState: CreateBookmarkState,
	onBookmarkEvent: (BookMarkEvents) -> Unit,
	modifier: Modifier = Modifier,
) {
	val scope = rememberCoroutineScope()
	val bookmarkSheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

	var isSheetOpen by remember { mutableStateOf(false) }

	if (isSheetOpen) {
		ModalBottomSheet(
			sheetState = bookmarkSheet,
			onDismissRequest = { isSheetOpen = false },
		) {
			AudioBookmarksList(
				bookmarks = bookmarks,
				onExportBookmarks = { onBookmarkEvent(BookMarkEvents.OnExportBookMarkPoints) },
				onEditBookMark = { onBookmarkEvent(BookMarkEvents.OpenDialogToEdit(it)) },
				onDeleteBookMark = { onBookmarkEvent(BookMarkEvents.OnDeleteBookmark(it)) },
				contentPadding = PaddingValues(all = dimensionResource(id = R.dimen.bottom_sheet_padding_lg)),
			)
		}
	}

	if (bookMarkState.showDialog) {
		BasicAlertDialog(
			onDismissRequest = { onBookmarkEvent(BookMarkEvents.OnCloseDialog) },
			properties = DialogProperties(dismissOnClickOutside = false)
		) {
			AddBookmarkDialogContent(
				isUpdate = bookMarkState.isUpdate,
				textFieldValue = bookMarkState.textValue,
				onValueChange = { onBookmarkEvent(BookMarkEvents.OnUpdateTextField(it)) },
				onDismiss = { onBookmarkEvent(BookMarkEvents.OnCloseDialog) },
				onConfirm = {
					val currentTime = trackCurrentTime()
					onBookmarkEvent(BookMarkEvents.OnAddOrUpdateBookMark(currentTime))
				},
			)
		}
	}

	Row(
		modifier = modifier,
		horizontalArrangement = Arrangement.SpaceBetween
	) {
		AssistChip(
			onClick = {
				scope.launch { bookmarkSheet.show() }
					.invokeOnCompletion { isSheetOpen = true }
			},
			label = {
				Text(
					text = stringResource(id = R.string.player_action_show_bookmarks),
					style = MaterialTheme.typography.labelMedium,
				)
			},
			leadingIcon = {
				Icon(
					painter = painterResource(R.drawable.ic_list),
					contentDescription = stringResource(id = R.string.player_action_show_bookmarks),
					modifier = Modifier.size(AssistChipDefaults.IconSize)
				)
			},
			shape = MaterialTheme.shapes.medium,
			colors = AssistChipDefaults.assistChipColors(
				containerColor = MaterialTheme.colorScheme.secondaryContainer,
				labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
				leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
			),
		)
		SuggestionChip(
			onClick = { onBookmarkEvent(BookMarkEvents.OpenDialogToCreate) },
			label = {
				Text(
					text = stringResource(R.string.player_action_add_bookmark),
					style = MaterialTheme.typography.labelMedium,
				)
			},
			icon = {
				Icon(
					painter = painterResource(id = R.drawable.ic_boomark_add),
					contentDescription = stringResource(id = R.string.player_action_add_bookmark),
					modifier = Modifier.size(SuggestionChipDefaults.IconSize)
				)
			},
			shape = MaterialTheme.shapes.medium,
			colors = SuggestionChipDefaults.suggestionChipColors(
				containerColor = MaterialTheme.colorScheme.tertiaryContainer,
				labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
				iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer
			),
		)
	}
}

@Composable
internal fun PlayerBookMarks(
	trackData: PlayerTrackData,
	bookmarks: ImmutableList<AudioBookmarkModel>,
	bookMarkState: CreateBookmarkState,
	onBookmarkEvent: (BookMarkEvents) -> Unit,
	modifier: Modifier = Modifier,
) {
	PlayerBookMarks(
		bookmarks = bookmarks,
		trackCurrentTime = { trackData.currentAsLocalTime },
		bookMarkState = bookMarkState,
		onBookmarkEvent = onBookmarkEvent,
		modifier = modifier.fillMaxWidth()
	)
}