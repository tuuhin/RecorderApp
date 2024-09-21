package com.eva.recorderapp.voice_recorder.presentation.recorder.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecorderTopBar(
	showActions: Boolean,
	modifier: Modifier = Modifier,
	onNavigateToBin: () -> Unit = {},
	onNavigateToSettings: () -> Unit = {},
	onNavigateToRecordings: () -> Unit = {},
	navigation: @Composable () -> Unit = {},
	onAddBookMark: () -> Unit = {},
	colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
) {
	var showDropDown by remember { mutableStateOf(false) }

	TopAppBar(
		title = { Text(text = stringResource(id = R.string.recorder_top_bar_title)) },
		navigationIcon = navigation,
		actions = {
			AnimatedContent(
				targetState = showActions,
				label = "Is normal action visible"
			) { isNormal ->
				if (isNormal) {
					TextButton(onClick = onNavigateToRecordings) {
						Text(
							text = stringResource(id = R.string.show_recordings_list),
							fontWeight = FontWeight.SemiBold
						)
					}
				} else {
					TooltipBox(
						positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
						tooltip = {
							RichTooltip {
								Text(text = stringResource(R.string.bookmark_tooltip_text))
							}
						},
						state = rememberTooltipState()
					) {
						TextButton(onClick = onAddBookMark) {
							Text(
								text = stringResource(id = R.string.add_recording_bookmark),
								fontWeight = FontWeight.SemiBold
							)
						}
					}
				}
			}
			AnimatedVisibility(
				visible = showActions,
				enter = slideInHorizontally() + fadeIn(),
				exit = slideOutHorizontally() + fadeOut()
			) {
				Box {
					IconButton(
						onClick = { showDropDown = !showDropDown },
						colors = IconButtonDefaults.iconButtonColors(contentColor = colors.actionIconContentColor)
					) {
						Icon(
							imageVector = Icons.Default.MoreVert,
							contentDescription = stringResource(id = R.string.menu_more_option)
						)
					}
					DropdownMenu(
						expanded = showDropDown,
						onDismissRequest = { showDropDown = false },
						shape = MaterialTheme.shapes.medium
					) {
						DropdownMenuItem(
							text = { Text(text = stringResource(id = R.string.menu_option_recycle_bin)) },
							onClick = onNavigateToBin,
							leadingIcon = {
								Icon(
									painter = painterResource(id = R.drawable.ic_recycle),
									contentDescription = stringResource(id = R.string.menu_option_recycle_bin)
								)
							},
						)
						DropdownMenuItem(
							text = { Text(text = stringResource(id = R.string.menu_option_settings)) },
							onClick = onNavigateToSettings,
							leadingIcon = {
								Icon(
									painter = painterResource(id = R.drawable.ic_settings),
									contentDescription = stringResource(id = R.string.menu_option_settings)
								)
							},
						)
					}
				}
			}
		},
		colors = colors,
		modifier = modifier,
	)
}

private class BooleanPreviewParams :
	CollectionPreviewParameterProvider<Boolean>(listOf(true, false))

@OptIn(ExperimentalMaterial3Api::class)
@PreviewLightDark
@Composable
private fun RecorderTopBarPreview(
	@PreviewParameter(BooleanPreviewParams::class)
	showActions: Boolean,
) = RecorderAppTheme {
	RecorderTopBar(showActions = showActions)
}