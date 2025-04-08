package com.eva.feature_recordings.bin.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.BottomAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.eva.ui.theme.RecorderAppTheme

@Composable
internal fun RecordingsBinScreenBottomBar(
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
		isVisible = true,
		onItemsDelete = { },
		onItemsRestore = { },
	)
}