package com.eva.feature_editor.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.eva.ui.R
import kotlin.math.exp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorTopBar(
	onSave: () -> Unit,
	modifier: Modifier = Modifier,
	navigation: @Composable () -> Unit = {},
	scrollBehavior: TopAppBarScrollBehavior? = null,
	colors: TopAppBarColors = TopAppBarDefaults
		.topAppBarColors(actionIconContentColor = MaterialTheme.colorScheme.primary),
) {

	var showDropDown by remember { mutableStateOf(false) }

	TopAppBar(
		title = { Text(text = stringResource(R.string.media_editor_title)) },
		navigationIcon = navigation,
		actions = {
			TextButton(onClick = onSave) {
				Text(text = stringResource(R.string.action_save))
			}
			Box {
				IconButton(onClick = { showDropDown = true }) {
					Icon(
						Icons.Default.MoreVert,
						contentDescription = stringResource(R.string.menu_more_option)
					)
				}
				DropdownMenu(
					expanded = showDropDown,
					onDismissRequest = { showDropDown = false },
					shape = MaterialTheme.shapes.medium,
				) {

				}
			}
		},
		modifier = modifier,
		scrollBehavior = scrollBehavior,
		colors = colors,
	)
}