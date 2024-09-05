package com.eva.recorderapp.voice_recorder.presentation.categories.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesBottomBar(
	isVisible: Boolean,
	showRename: Boolean,
	onRename: () -> Unit,
	onDelete: () -> Unit,
	modifier: Modifier = Modifier
) {
	AnimatedVisibility(
		visible = isVisible,
		modifier = modifier,
	) {
		BottomAppBar(
			actions = {
				AnimatedVisibility(
					visible = showRename,
					enter = slideInHorizontally() + fadeIn(),
					exit = slideOutHorizontally() + fadeOut()
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
								painter = painterResource(id = R.drawable.ic_edit),
								contentDescription = stringResource(id = R.string.player_action_edit)
							)
						}
					}
				}
			},
			floatingActionButton = {
				DeleteCategoryButton(
					onDeleteAfterWarn = onDelete,
					shape = MaterialTheme.shapes.medium
				)
			},
			tonalElevation = 2.dp,
		)
	}
}

@PreviewLightDark
@Composable
private fun CategoriesBottomBarPreview() = RecorderAppTheme {
	CategoriesBottomBar(
		isVisible = true,
		showRename = true,
		onDelete = {},
		onRename = {}
	)
}