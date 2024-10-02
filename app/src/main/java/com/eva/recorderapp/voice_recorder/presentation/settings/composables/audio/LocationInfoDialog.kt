package com.eva.recorderapp.voice_recorder.presentation.settings.composables.audio

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme

@Composable
fun LocationInfoDialog(
	showDialog: Boolean,
	onDismiss: () -> Unit,
	onConfirm: () -> Unit,
	modifier: Modifier = Modifier,
) {
	if (!showDialog) return

	AlertDialog(
		onDismissRequest = onDismiss,
		confirmButton = {
			Button(onClick = onConfirm) {
				Text(text = stringResource(R.string.action_request_permission))
			}
		},
		dismissButton = {
			TextButton(onClick = onDismiss) {
				Text(text = stringResource(R.string.action_cancel))
			}
		},
		text = { Text(text = stringResource(id = R.string.recording_settings_add_location_info_text_explained)) },
		title = { Text(text = stringResource(id = R.string.recording_settings_add_location_info_title)) },
		icon = {
			Icon(
				painter = painterResource(id = R.drawable.ic_location),
				contentDescription = stringResource(id = R.string.recording_settings_add_location_info_title),
			)
		},
		shape = MaterialTheme.shapes.extraLarge,
		modifier = modifier,
	)
}

@PreviewLightDark
@Composable
private fun LocationInfoDialogPreview() = RecorderAppTheme {
	LocationInfoDialog(showDialog = true, onConfirm = {}, onDismiss = {})
}