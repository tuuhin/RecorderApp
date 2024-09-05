package com.eva.recorderapp.voice_recorder.presentation.categories.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.presentation.recordings.composable.animateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesTopBar(
	isCategorySelected: Boolean,
	selectedCount: Int,
	onCreate: () -> Unit,
	onSelectAll: () -> Unit,
	onUnSelectAll: () -> Unit,
	modifier: Modifier = Modifier,
	scrollBehavior: TopAppBarScrollBehavior? = null,
	navigation: @Composable () -> Unit = {},
	colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
) {
	AnimatedContent(
		targetState = isCategorySelected,
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
								imageVector = Icons.Outlined.DoneAll,
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
				title = { Text(text = stringResource(id = R.string.manage_categories_top_bar_title)) },
				navigationIcon = navigation,
				colors = colors,
				scrollBehavior = scrollBehavior,
				actions = {
					TextButton(
						onClick = onCreate,
						colors = ButtonDefaults
							.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
					) {
						Text(text = stringResource(R.string.menu_option_create))
					}
				}
			)
		}
	}
}