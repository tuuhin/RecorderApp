package com.eva.feature_recordings.recordings.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.eva.feature_recordings.bin.composables.TrashSelectedRecordingsButton
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecordingsBottomBar(
	onItemDelete: () -> Unit,
	isVisible: Boolean,
	modifier: Modifier = Modifier,
	showRename: Boolean = false,
	onRename: () -> Unit = {},
	onMoveToCategory: () -> Unit = {},
	onShareSelected: () -> Unit = {},
	onStarItem: () -> Unit = {},
) {
	var showDropDown by remember { mutableStateOf(false) }

	AnimatedVisibility(
		visible = isVisible,
		enter = expandIn(
			animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
			expandFrom = Alignment.TopCenter
		) + slideInVertically(
			animationSpec = tween(durationMillis = 400),
			initialOffsetY = { height -> height },
		),
		exit = shrinkOut(
			animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
			shrinkTowards = Alignment.TopCenter
		) + slideOutVertically(
			animationSpec = tween(durationMillis = 400),
			targetOffsetY = { height -> -height },
		),
		modifier = modifier.fillMaxWidth(),
	) {
		BottomAppBar(
			actions = {
				Box {
					TooltipBox(
						positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
						tooltip = {
							PlainTooltip {
								Text(text = stringResource(id = R.string.menu_more_option))
							}
						},
						state = rememberTooltipState(),
					) {
						IconButton(onClick = { showDropDown = true }) {
							Icon(
								imageVector = Icons.Default.MoreVert,
								contentDescription = stringResource(id = R.string.menu_more_option)
							)
						}
					}
					DropdownMenu(
						expanded = isVisible && showDropDown,
						onDismissRequest = { showDropDown = false },
						shape = MaterialTheme.shapes.large,
					) {
						AnimatedVisibility(
							visible = showRename,
						) {
							DropdownMenuItem(
								text = { Text(text = stringResource(id = R.string.rename_recording_action)) },
								onClick = onRename,
								leadingIcon = {
									Icon(
										painter = painterResource(id = R.drawable.ic_edit),
										contentDescription = stringResource(id = R.string.rename_recording_action),
									)
								},
							)
						}
						DropdownMenuItem(
							text = { Text(text = stringResource(id = R.string.menu_option_categories)) },
							onClick = onMoveToCategory,
							leadingIcon = {
								Icon(
									painter = painterResource(id = R.drawable.ic_folder),
									contentDescription = stringResource(id = R.string.menu_option_categories)
								)
							},
						)
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
							painter = painterResource(R.drawable.ic_share),
							contentDescription = stringResource(id = R.string.action_share),
						)
					}
				}
				TooltipBox(
					positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
					tooltip = {
						PlainTooltip {
							Text(text = stringResource(id = R.string.menu_option_favourite))
						}
					},
					state = rememberTooltipState()
				) {
					IconButton(onClick = onStarItem) {
						Icon(
							painter = painterResource(R.drawable.ic_star_outlined),
							contentDescription = stringResource(id = R.string.menu_option_favourite),
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
		)
	}
}

@PreviewLightDark
@Composable
private fun RecordingsBottomBarPreview(
	@PreviewParameter(BooleanPreviewParams::class)
	showRename: Boolean,
) = RecorderAppTheme {
	RecordingsBottomBar(
		isVisible = true,
		showRename = showRename,
		onShareSelected = {},
		onItemDelete = {}
	)
}