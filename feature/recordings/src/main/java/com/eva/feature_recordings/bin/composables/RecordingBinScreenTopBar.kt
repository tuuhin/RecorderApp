package com.eva.feature_recordings.bin.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eva.feature_recordings.recordings.composables.animateTopBar
import com.eva.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecordingBinScreenTopBar(
	isSelectedMode: Boolean,
	selectedCount: Int,
	onUnSelectAll: () -> Unit,
	onSelectAll: () -> Unit,
	modifier: Modifier = Modifier,
	scrollBehavior: TopAppBarScrollBehavior? = null,
	navigation: @Composable () -> Unit = {},
	colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
		actionIconContentColor = MaterialTheme.colorScheme.primary
	),
) {
	AnimatedContent(
		targetState = isSelectedMode,
		transitionSpec = { animateTopBar() },
		label = "Selectable Top bar animation",
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
								painter = painterResource(R.drawable.ic_done_all),
								contentDescription = stringResource(id = R.string.select_all_action)
							)
						}
					}
				},
				colors = colors.copy(
					containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
				)
			)
		} else {
			MediumTopAppBar(
				title = { Text(text = stringResource(id = R.string.recording_bin_top_bar_title)) },
				scrollBehavior = scrollBehavior,
				navigationIcon = navigation,
				colors = colors,
			)
		}
	}
}