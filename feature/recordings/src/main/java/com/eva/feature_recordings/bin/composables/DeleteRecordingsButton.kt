package com.eva.feature_recordings.bin.composables

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.Icon
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
import com.eva.ui.R

@Composable
internal fun DeleteRecordingsButton(
	onDelete: () -> Unit,
	modifier: Modifier = Modifier,
	shape: Shape = FloatingActionButtonDefaults.shape,
	containerColor: Color = FloatingActionButtonDefaults.containerColor,
	contentColor: Color = contentColorFor(containerColor),
	elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
) {

	var showDialog by remember { mutableStateOf(false) }

	if (showDialog)
		AlertDialog(
			onDismissRequest = { showDialog = false },
			confirmButton = {
				TextButton(onClick = onDelete) {
					Text(text = stringResource(id = R.string.recording_action_delete))
				}
			},
			dismissButton = {
				TextButton(onClick = { showDialog = false }) {
					Text(text = stringResource(id = R.string.action_cancel))
				}
			},
			title = {
				Text(text = stringResource(id = R.string.recording_permanent_delete_dialog_title))
			},
			text = {
				Text(text = stringResource(id = R.string.recording_permanent_delete_dialog_text))
			},
			icon = {
				Icon(
					painter = painterResource(id = R.drawable.ic_delete),
					contentDescription = stringResource(R.string.recording_action_delete)
				)
			},
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
			painter = painterResource(id = R.drawable.ic_delete),
			contentDescription = stringResource(id = R.string.recording_action_delete)
		)
		Spacer(modifier = Modifier.width(6.dp))
		Text(text = stringResource(id = R.string.recording_action_delete))
	}
}