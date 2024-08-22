package com.eva.recorderapp.voice_recorder.presentation.recordings.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.surfaceColorAtElevation
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
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsScreenTopBar(
	isSelectedMode: Boolean,
	selectedCount: Int,
	onUnSelectAll: () -> Unit,
	onSelectAll: () -> Unit,
	modifier: Modifier = Modifier,
	scrollBehavior: TopAppBarScrollBehavior? = null,
	navigation: @Composable () -> Unit = {},
	onNavigateToBin: () -> Unit = {},
	onSortItems: () -> Unit = {},
	colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors()
) {

	var showDropDown by remember { mutableStateOf(false) }

	AnimatedContent(
		targetState = isSelectedMode,
		transitionSpec = { animateTopbar() },
		label = "Selectable Topbar animation",
		contentAlignment = Alignment.TopCenter,
		modifier = modifier,
	) { isSelected ->
		if (isSelected) {
			MediumTopAppBar(
				title = {
					Text(text = stringResource(R.string.selected_recording_count, selectedCount))
				},
				navigationIcon = {
					TooltipBox(
						positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
						tooltip = {
							PlainTooltip {
								Text(text = stringResource(id = R.string.cancel_selection))
							}
						},
						state = rememberTooltipState()
					) {
						IconButton(onClick = onUnSelectAll) {
							Icon(
								imageVector = Icons.Outlined.Close,
								contentDescription = stringResource(id = R.string.cancel_selection)
							)
						}
					}
				},
				actions = {
					TooltipBox(
						positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
						tooltip = {
							PlainTooltip {
								Text(text = stringResource(id = R.string.select_all_action))
							}
						},
						state = rememberTooltipState(),
					) {
						IconButton(onClick = onSelectAll) {
							Icon(
								imageVector = Icons.Outlined.DoneAll,
								contentDescription = stringResource(id = R.string.select_all_action)
							)
						}
					}
				},
				colors = colors.copy(
					containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
				),
				scrollBehavior = scrollBehavior,
			)
		} else MediumTopAppBar(
			title = { Text(text = stringResource(id = R.string.recording_top_bar_title)) },
			actions = {
				IconButton(onClick = { }) {
					Icon(imageVector = Icons.Outlined.Search, contentDescription = "")
				}

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
						expanded = showDropDown,
						onDismissRequest = { showDropDown = false },
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
							text = { Text(text = stringResource(id = R.string.menu_option_sort_order)) },
							onClick = onSortItems,
							leadingIcon = {
								Icon(
									painter = painterResource(id = R.drawable.ic_sort),
									contentDescription = stringResource(id = R.string.menu_option_sort_order)
								)
							},
						)
					}
				}
			},
			scrollBehavior = scrollBehavior,
			navigationIcon = navigation,
			colors = colors,
		)
	}
}

fun AnimatedContentTransitionScope<Boolean>.animateTopbar(): ContentTransform {
	val enterIn = expandIn(
		animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
		expandFrom = Alignment.TopCenter
	) + slideInVertically(
		animationSpec = tween(durationMillis = 400),
		initialOffsetY = { height -> height },
	)

	val exitOut = shrinkOut(
		animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
		shrinkTowards = Alignment.TopCenter
	) + slideOutVertically(
		animationSpec = tween(durationMillis = 400),
		targetOffsetY = { height -> -height },
	)
	return enterIn togetherWith exitOut
}

private class BooleanPreivewParams :
	CollectionPreviewParameterProvider<Boolean>(listOf(true, false))


@OptIn(ExperimentalMaterial3Api::class)
@PreviewLightDark
@Composable
private fun RecordingsTopBarSelectedPreview(
	@PreviewParameter(BooleanPreivewParams::class)
	isSelectedMode: Boolean
) = RecorderAppTheme {
	RecordingsScreenTopBar(
		isSelectedMode = isSelectedMode,
		selectedCount = 10,
		onUnSelectAll = {},
		onSelectAll = { },
		navigation = {
			Icon(
				imageVector = Icons.AutoMirrored.Default.ArrowBack,
				contentDescription = stringResource(R.string.back_arrow)
			)
		}
	)
}
