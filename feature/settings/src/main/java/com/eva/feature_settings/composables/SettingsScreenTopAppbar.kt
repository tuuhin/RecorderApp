package com.eva.feature_settings.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.eva.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenTopAppbar(
	modifier: Modifier = Modifier,
	navigation: @Composable () -> Unit,
	onNavigateToInfo: () -> Unit = {},
	scrollBehavior: TopAppBarScrollBehavior? = null
) {

	var showMenu by remember { mutableStateOf(false) }

	TopAppBar(
		title = { Text(text = stringResource(id = R.string.app_settings_common)) },
		navigationIcon = navigation,
		actions = {
			Box {
				IconButton(onClick = { showMenu = true }) {
					Icon(
						imageVector = Icons.Outlined.MoreVert,
						contentDescription = stringResource(R.string.extras_info)
					)
				}
				DropdownMenu(
					expanded = showMenu,
					onDismissRequest = { showMenu = false },
					shape = MaterialTheme.shapes.large,
					containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
				) {
					DropdownMenuItem(
						text = { Text(stringResource(R.string.app_settings_extra_option_about)) },
						leadingIcon = {
							Icon(
								imageVector = Icons.Outlined.Info,
								contentDescription = stringResource(R.string.extras_info)
							)
						},
						onClick = {
							showMenu = false
							onNavigateToInfo()
						}
					)
				}
			}
		},
		scrollBehavior = scrollBehavior,
		modifier = modifier,
	)
}