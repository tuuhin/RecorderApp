package com.eva.recorderapp.voice_recorder.presentation.recordings.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.SelectableRecordings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun RecordingsBinScreenBottomBar(
	recordings: ImmutableList<SelectableRecordings>,
	isVisible: Boolean,
	onItemsDelete: () -> Unit,
	onItemsRestore: () -> Unit,
	modifier: Modifier = Modifier,
) {
	AnimatedVisibility(
		visible = isVisible,
		enter = slideInVertically(),
		exit = slideOutVertically(),
	) {
		BottomAppBar(
			modifier = modifier,
			actions = {
				IconButton(onClick = onItemsRestore) {
					Icon(
						imageVector = Icons.Default.Restore,
						contentDescription = stringResource(id = R.string.recordings_restore_action)
					)
				}
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