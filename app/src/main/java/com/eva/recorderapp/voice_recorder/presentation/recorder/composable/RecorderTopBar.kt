package com.eva.recorderapp.voice_recorder.presentation.recorder.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.eva.recorderapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecorderTopBar(
	showActions: Boolean,
	onShowRecordings: () -> Unit,
	onNavigateToSettings: () -> Unit,
	onNavigateToBin: () -> Unit,
	modifier: Modifier = Modifier,
	navigation: @Composable () -> Unit = {},
) {
	var showDropDown by remember { mutableStateOf(false) }

	TopAppBar(
		title = { Text(text = stringResource(id = R.string.recorder_top_bar_title)) },
		navigationIcon = navigation,
		actions = {
			AnimatedVisibility(
				visible = showActions,
				enter = fadeIn() + slideInVertically(),
				exit = slideOutVertically() + fadeOut()
			) {
				TextButton(onClick = onShowRecordings) {
					Text(
						text = stringResource(id = R.string.show_recordings_list),
						fontWeight = FontWeight.SemiBold
					)
				}
			}
			AnimatedVisibility(
				visible = showActions,
				enter = fadeIn() + slideInVertically(),
				exit = slideOutVertically() + fadeOut()
			) {
				Box {
					IconButton(onClick = { showDropDown = !showDropDown }) {
						Icon(
							imageVector = Icons.Default.MoreVert,
							contentDescription = stringResource(id = R.string.menu_more_option)
						)
					}
					DropdownMenu(
						expanded = showDropDown,
						onDismissRequest = { showDropDown = false }
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
		colors = TopAppBarDefaults
			.topAppBarColors(actionIconContentColor = MaterialTheme.colorScheme.primary),
		modifier = modifier,
	)
}