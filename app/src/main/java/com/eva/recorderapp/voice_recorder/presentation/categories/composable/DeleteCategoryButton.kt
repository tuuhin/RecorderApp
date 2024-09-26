package com.eva.recorderapp.voice_recorder.presentation.categories.composable

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R

@Composable
fun DeleteCategoryButton(
	onDeleteAfterWarn: () -> Unit,
	modifier: Modifier = Modifier,
	shape: Shape = MaterialTheme.shapes.medium,
	containerColor: Color = FloatingActionButtonDefaults.containerColor,
	contentColor: Color = contentColorFor(containerColor),
	elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
) {
	var showDialog by remember { mutableStateOf(false) }

	if (showDialog)
		AlertDialog(
			onDismissRequest = { showDialog = false },
			confirmButton = {
				TextButton(
					onClick = {
						showDialog = false
						onDeleteAfterWarn()
					},
				) {
					Text(text = stringResource(id = R.string.recording_action_delete))
				}
			},
			dismissButton = {
				TextButton(onClick = { showDialog = false }) {
					Text(text = stringResource(id = R.string.action_cancel))
				}
			},
			title = { Text(text = stringResource(id = R.string.delete_category_dialog_title)) },
			text = { Text(text = stringResource(id = R.string.delete_category_dialog_text)) },
			icon = {
				Icon(
					painter = painterResource(id = R.drawable.ic_delete),
					contentDescription = stringResource(id = R.string.recording_action_trash)
				)
			},
			modifier = modifier,
		)

	ExtendedFloatingActionButton(
		onClick = { showDialog = true },
		modifier = modifier,
		shape = shape,
		contentColor = contentColor,
		containerColor = containerColor,
		elevation = elevation
	) {
		Icon(
			imageVector = Icons.Outlined.Delete,
			contentDescription = stringResource(R.string.menu_option_delete)
		)
		Spacer(modifier = Modifier.width(2.dp))
		Text(text = stringResource(R.string.menu_option_delete))
	}
}