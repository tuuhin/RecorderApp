package com.eva.recorderapp.voice_recorder.presentation.recordings.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.SelectableRecordings
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import kotlinx.collections.immutable.ImmutableList


private val bottomBarAnimationSpec = tween<IntOffset>(
	durationMillis = 600,
	easing = EaseInOut
)

@Composable
fun RecordingsBottomBar(
	recordings: ImmutableList<SelectableRecordings>,
	isVisible: Boolean,
	onItemDelete: () -> Unit,
	modifier: Modifier = Modifier,
	showRename: Boolean = false,
) {
	AnimatedVisibility(
		visible = isVisible,
		enter = slideInVertically(),
		exit = slideOutVertically(),
	) {
		BottomAppBar(
			actions = {
				IconButton(onClick = { }) {
					Icon(
						Icons.Outlined.Category,
						contentDescription = "Add category",
					)
				}
				IconButton(onClick = { }) {
					Icon(
						imageVector = Icons.Outlined.StarOutline,
						contentDescription = "Favourites"
					)
				}
				if (showRename) {
					IconButton(onClick = { /* do something */ }) {
						Icon(
							Icons.Outlined.Edit,
							contentDescription = "Rename",
						)
					}
				}
				IconButton(onClick = { /* do something */ }) {
					Icon(
						Icons.Outlined.Share,
						contentDescription = "Share",
					)
				}
			},
			floatingActionButton = {
				TrashSelectedRecordingsButton(
					recordings = recordings,
					onDelete = onItemDelete
				)
			},
			tonalElevation = 2.dp,
			modifier = modifier
		)
	}
}

@PreviewLightDark
@Composable
private fun RecorginsBottomBarPreview() = RecorderAppTheme {
	RecordingsBottomBar(
		recordings = PreviewFakes.FAKE_VOICE_RECORDING_MODELS,
		isVisible = true,
		onItemDelete = {})
}