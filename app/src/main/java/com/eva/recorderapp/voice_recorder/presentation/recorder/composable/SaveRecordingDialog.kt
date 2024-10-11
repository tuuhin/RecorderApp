package com.eva.recorderapp.voice_recorder.presentation.recorder.composable

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.window.DialogProperties
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme

@Composable
fun SaveRecordingDialog(
	showDialog: Boolean,
	onDismiss: () -> Unit,
	onSave: () -> Unit,
	modifier: Modifier = Modifier,
	properties: DialogProperties = DialogProperties(dismissOnClickOutside = false),
) {
	if (!showDialog) return

	AlertDialog(
		onDismissRequest = onDismiss,
		confirmButton = {
			TextButton(
				onClick = {
					onDismiss()
					onSave()
				},
			) {
				Text(text = stringResource(id = R.string.dialog_action_done))
			}
		},
		dismissButton = {
			TextButton(onClick = onDismiss) {
				Text(text = stringResource(id = R.string.action_cancel))
			}
		},
		title = { Text(text = stringResource(id = R.string.save_recording_dialog_title)) },
		text = {
			Text(
				text = stringResource(id = R.string.save_recording_dialog_text),
				textAlign = TextAlign.Center
			)
		},
		modifier = modifier,
		shape = MaterialTheme.shapes.extraLarge,
		properties = properties,
	)
}

@PreviewLightDark
@Composable
private fun SaveRecordingsDialogPreview() = RecorderAppTheme {
	SaveRecordingDialog(
		showDialog = true,
		onDismiss = {},
		onSave = {}
	)
}