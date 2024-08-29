package com.eva.recorderapp.voice_recorder.presentation.recorder.composable

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.window.DialogProperties
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme

@Composable
fun CancelRecordingDialog(
	showDialog: Boolean,
	onDismiss: () -> Unit,
	onDiscard: () -> Unit,
	modifier: Modifier = Modifier,
	properties: DialogProperties = DialogProperties(dismissOnClickOutside = false)
) {
	if (!showDialog) return

	AlertDialog(
		onDismissRequest = onDismiss,
		confirmButton = {
			TextButton(
				onClick = {
					onDismiss()
					onDiscard()
				},
			) {
				Text(text = stringResource(id = R.string.dialog_action_discard))
			}

		},
		dismissButton = {
			TextButton(onClick = onDismiss) {
				Text(text = stringResource(id = R.string.action_cancel))
			}
		},
		title = { Text(text = stringResource(id = R.string.cancel_recording_dialog_title)) },
		text = {
			Text(text = stringResource(id = R.string.cancel_recording_dialog_text))
		},
		icon = {
			Icon(
				painter = painterResource(id = R.drawable.ic_close),
				contentDescription = stringResource(id = R.string.recorder_action_cancel)
			)
		},
		modifier = modifier,
		shape = MaterialTheme.shapes.large,
		properties = properties
	)
}

@PreviewLightDark
@Composable
private fun CancelRecordingDialogPreview() = RecorderAppTheme {
	CancelRecordingDialog(
		showDialog = true,
		onDismiss = { },
		onDiscard = { },
	)
}