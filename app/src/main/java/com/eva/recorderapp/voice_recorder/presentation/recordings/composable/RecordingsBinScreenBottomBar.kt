package com.eva.recorderapp.voice_recorder.presentation.recordings.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.BottomAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.SelectableTrashRecordings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun RecordingsBinScreenBottomBar(
	recordings: ImmutableList<SelectableTrashRecordings>,
	isVisible: Boolean,
	onItemsDelete: () -> Unit,
	onItemsRestore: () -> Unit,
	modifier: Modifier = Modifier,
) {
	AnimatedVisibility(
		visible = isVisible,
		enter = slideInVertically() + fadeIn(),
		exit = slideOutVertically() + fadeOut(),
	) {
		BottomAppBar(
			modifier = modifier,
			actions = {
				RestoreRecordingsButton(
					onItemRestore = onItemsRestore
				)
			},
			floatingActionButton = {
				DeleteRecordingsButton(
					recordings = recordings,
					onDelete = onItemsDelete
				)
			},
		)
	}

}


@PreviewLightDark
@Composable
private fun RecordingsBinScreenBottomBarPreview() = RecorderAppTheme {
	RecordingsBinScreenBottomBar(
		recordings = persistentListOf(),
		isVisible = true,
		onItemsDelete = { },
		onItemsRestore = { },
	)
}