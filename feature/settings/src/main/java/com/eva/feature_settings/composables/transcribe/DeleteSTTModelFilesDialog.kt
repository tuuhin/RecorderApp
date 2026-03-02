package com.eva.feature_settings.composables.transcribe

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eva.ui.R

@Composable
internal fun DeleteSTTModelFilesDialog(
	showDialog: Boolean,
	modelId: String,
	formattedSize: String,
	onConfirm: () -> Unit,
	onDismiss: () -> Unit,
	modifier: Modifier = Modifier,
	shape: Shape = AlertDialogDefaults.shape,
	tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
	containerColor: Color = AlertDialogDefaults.containerColor,
	titleContentColor: Color = AlertDialogDefaults.titleContentColor,
	textContentColor: Color = AlertDialogDefaults.textContentColor,
	iconContentColor: Color = AlertDialogDefaults.iconContentColor,
) {

	if (!showDialog) return

	AlertDialog(
		onDismissRequest = onDismiss,
		confirmButton = {
			TextButton(
				onClick = onConfirm,
				colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
			) {
				Text(text = stringResource(R.string.recording_action_delete))
			}
		},
		dismissButton = {
			TextButton(
				onClick = onDismiss,
				colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
			) {
				Text(text = stringResource(R.string.action_cancel))
			}
		},
		icon = {
			Icon(
				painter = painterResource(R.drawable.ic_delete),
				contentDescription = null,
				modifier = Modifier.size(32.dp)
			)
		},
		title = { Text(text = stringResource(R.string.recording_action_delete)) },
		text = {
			Text(text = stringResource(R.string.delete_stt_model_dialog_text))
		},
		modifier = modifier,
		shape = shape,
		tonalElevation = tonalElevation,
		containerColor = containerColor,
		titleContentColor = titleContentColor,
		textContentColor = textContentColor,
		iconContentColor = iconContentColor,
	)
}