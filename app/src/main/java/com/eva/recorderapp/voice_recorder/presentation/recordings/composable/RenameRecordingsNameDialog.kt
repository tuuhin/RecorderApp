package com.eva.recorderapp.voice_recorder.presentation.recordings.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.event.RenameRecordingEvents
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.RenameRecordingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameRecordingNameDialog(
	showDialog: Boolean,
	value: TextFieldValue,
	onValueChange: (TextFieldValue) -> Unit,
	onDismissRequest: () -> Unit,
	modifier: Modifier = Modifier,
	isReanaming: Boolean = false,
	onCancel: () -> Unit = {},
	onRename: () -> Unit = {},
	error: Boolean = false,
	properties: DialogProperties = DialogProperties()
) {
	if (!showDialog) return

	BasicAlertDialog(
		onDismissRequest = onDismissRequest,
		modifier = modifier,
		properties = properties
	) {
		Surface(
			shape = AlertDialogDefaults.shape,
			tonalElevation = AlertDialogDefaults.TonalElevation,
			color = AlertDialogDefaults.containerColor,
			contentColor = contentColorFor(AlertDialogDefaults.containerColor)
		) {
			Column(modifier = Modifier.padding(24.dp)) {
				Text(
					text = stringResource(id = R.string.rename_recording_dialog_title),
					style = MaterialTheme.typography.headlineSmall,
					color = AlertDialogDefaults.titleContentColor,
					modifier = Modifier.padding(12.dp)
				)
				Text(
					text = stringResource(id = R.string.rename_recording_dialog_text),
					color = AlertDialogDefaults.textContentColor,
					style = MaterialTheme.typography.bodyMedium,
					modifier = Modifier.padding(vertical = 6.dp),
				)
				OutlinedTextField(
					value = value,
					onValueChange = onValueChange,
					label = { Text(text = stringResource(id = R.string.rename_label_text)) },
					isError = error,
					shape = MaterialTheme.shapes.large,
					keyboardActions = KeyboardActions(onDone = { onRename() }),
					keyboardOptions = KeyboardOptions(
						keyboardType = KeyboardType.Text,
						imeAction = ImeAction.Done
					)
				)
				AnimatedVisibility(
					visible = error,
					enter = slideInVertically(),
					exit = slideOutVertically()
				) {
					Text(text = stringResource(id = R.string.rename_error_text))
				}
				Spacer(modifier = Modifier.height(12.dp))
				Row(
					modifier = Modifier.align(Alignment.End),
					horizontalArrangement = Arrangement.spacedBy(6.dp)
				) {
					TextButton(onClick = onCancel) {
						Text(text = stringResource(id = R.string.action_cancel))
					}
					Button(onClick = onRename, enabled = !isReanaming) {
						Text(text = stringResource(id = R.string.rename_recording_action))
					}
				}

			}
		}
	}
}

@Composable
fun RenameRecordingsNameDialog(
	state: RenameRecordingState,
	onEvent: (RenameRecordingEvents) -> Unit,
	modifier: Modifier = Modifier
) {
	RenameRecordingNameDialog(
		showDialog = state.showDialog,
		value = state.textFieldState,
		error = state.isBlank,
		onValueChange = { onEvent(RenameRecordingEvents.OnTextValueChange(it)) },
		onDismissRequest = { onEvent(RenameRecordingEvents.OnCancelRenameRecording) },
		onCancel = { onEvent(RenameRecordingEvents.OnCancelRenameRecording) },
		onRename = { onEvent(RenameRecordingEvents.OnRenameRecording) },
		modifier = modifier
	)
}

@PreviewLightDark
@Composable
private fun RenameRecordingsDialogPreview() = RecorderAppTheme {
	RenameRecordingNameDialog(
		showDialog = true,
		value = TextFieldValue(),
		onValueChange = {},
		onDismissRequest = { })
}