package com.eva.feature_editor.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
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
import com.eva.feature_editor.undoredo.UndoRedoState
import com.eva.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorTopBar(
	onExport: () -> Unit,
	modifier: Modifier = Modifier,
	navigation: @Composable () -> Unit = {},
	scrollBehavior: TopAppBarScrollBehavior? = null,
	isActionsEnabled: Boolean = true,
	state: UndoRedoState = UndoRedoState(),
	onUndoAction: () -> Unit = {},
	onRedoAction: () -> Unit = {},
	colors: TopAppBarColors = TopAppBarDefaults
		.topAppBarColors(actionIconContentColor = MaterialTheme.colorScheme.primary),
) {

	var showDropDown by remember { mutableStateOf(false) }

	TopAppBar(
		title = { Text(text = stringResource(R.string.media_editor_title)) },
		navigationIcon = navigation,
		actions = {
			TextButton(
				onClick = onExport,
				enabled = isActionsEnabled
			) {
				Text(text = stringResource(R.string.action_save))
			}
			Box {
				IconButton(onClick = { showDropDown = true }) {
					Icon(
						imageVector = Icons.Default.MoreVert,
						contentDescription = stringResource(R.string.menu_more_option)
					)
				}
				DropdownMenu(
					expanded = showDropDown,
					onDismissRequest = { showDropDown = false },
					shape = MaterialTheme.shapes.medium,
				) {
					DropdownMenuItem(
						text = { Text(text = stringResource(R.string.action_undo)) },
						enabled = state.canUndo,
						leadingIcon = {
							Icon(
								Icons.AutoMirrored.Filled.Undo,
								contentDescription = "Undo Action"
							)
						},
						onClick = onUndoAction
					)
					DropdownMenuItem(
						text = { Text(text = stringResource(R.string.action_redo)) },
						enabled = state.canRedo,
						leadingIcon = {
							Icon(
								Icons.AutoMirrored.Filled.Redo,
								contentDescription = "Redo Action"
							)
						},
						onClick = onRedoAction
					)
				}
			}
		},
		modifier = modifier,
		scrollBehavior = scrollBehavior,
		colors = colors,
	)
}