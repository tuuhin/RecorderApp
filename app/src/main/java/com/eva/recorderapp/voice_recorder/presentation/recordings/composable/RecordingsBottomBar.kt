package com.eva.recorderapp.voice_recorder.presentation.recordings.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme


private val bottomBarAnimationSpec = tween<IntOffset>(
	durationMillis = 600,
	easing = EaseInOut
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsBottomBar(
	onItemDelete: () -> Unit,
	isVisible: Boolean,
	modifier: Modifier = Modifier,
	showRename: Boolean = false,
	onRename: () -> Unit = {},
	onShareSelected: () -> Unit = {},
) {
	AnimatedVisibility(
		visible = isVisible,
		enter = slideInVertically(),
		exit = slideOutVertically(),
	) {
		BottomAppBar(
			actions = {
				AnimatedVisibility(
					visible = showRename,
					enter = slideInHorizontally(animationSpec = bottomBarAnimationSpec),
					exit = slideOutHorizontally(animationSpec = bottomBarAnimationSpec)
				) {
					TooltipBox(
						positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
						tooltip = {
							PlainTooltip {
								Text(text = stringResource(id = R.string.rename_recording_action))
							}
						},
						state = rememberTooltipState()
					) {
						IconButton(onClick = onRename) {
							Icon(
								imageVector = Icons.Outlined.Edit,
								contentDescription = stringResource(id = R.string.rename_recording_action),
							)
						}
					}
				}
				TooltipBox(
					positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
					tooltip = {
						PlainTooltip {
							Text(text = stringResource(id = R.string.action_share))
						}
					},
					state = rememberTooltipState()
				) {
					IconButton(onClick = onShareSelected) {
						Icon(
							Icons.Outlined.Share,
							contentDescription = stringResource(id = R.string.action_share),
						)
					}
				}
			},
			floatingActionButton = {
				TrashSelectedRecordingsButton(
					onDelete = onItemDelete
				)
			},
			tonalElevation = 2.dp,
			modifier = modifier,
		)
	}
}

@PreviewLightDark
@Composable
private fun RecordingsBottomBarPreview() = RecorderAppTheme {
	RecordingsBottomBar(
		isVisible = true,
		onShareSelected = {},
		onItemDelete = {}
	)
}