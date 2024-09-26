package com.eva.recorderapp.voice_recorder.presentation.recordings.rename_dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme

@Composable
fun RenameRecordingsDialogContent(
	state: RenameRecordingState,
	onEvent: (RenameRecordingEvent) -> Unit,
	onDismissRequest: () -> Unit,
	modifier: Modifier = Modifier,
) {
	Box(
		modifier = modifier.sizeIn(
			minWidth = dimensionResource(R.dimen.dialog_min_constraint_width),
			maxWidth = dimensionResource(R.dimen.dialog_min_constraint_height)
		),
		propagateMinConstraints = true,
	) {
		RenameRecordingDialogContent(
			value = state.textFieldState,
			errorMessage = state.errorString,
			hasError = state.hasError,
			onValueChange = { onEvent(RenameRecordingEvent.OnTextValueChange(it)) },
			onRename = { onEvent(RenameRecordingEvent.OnRenameRecording) },
			onCancel = onDismissRequest,
			modifier = modifier,
		)
	}
}


@Composable
private fun RenameRecordingDialogContent(
	value: TextFieldValue,
	onValueChange: (TextFieldValue) -> Unit,
	onCancel: () -> Unit,
	modifier: Modifier = Modifier,
	isRenaming: Boolean = false,
	onRename: () -> Unit = {},
	errorMessage: String = "",
	hasError: Boolean = false,
) {
	Surface(
		shape = AlertDialogDefaults.shape,
		tonalElevation = AlertDialogDefaults.TonalElevation,
		color = AlertDialogDefaults.containerColor,
		contentColor = contentColorFor(AlertDialogDefaults.containerColor),
		modifier = modifier
	) {
		Column(
			modifier = Modifier.padding(24.dp)
		) {
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
				isError = hasError,
				shape = MaterialTheme.shapes.medium,
				keyboardActions = KeyboardActions(onDone = { onRename() }),
				keyboardOptions = KeyboardOptions(
					keyboardType = KeyboardType.Text,
					imeAction = ImeAction.Done
				),
				modifier = Modifier.fillMaxWidth()
			)
			AnimatedVisibility(
				visible = hasError,
				enter = slideInVertically(),
				exit = slideOutVertically()
			) {
				Text(text = errorMessage)
			}
			Spacer(modifier = Modifier.height(12.dp))
			Row(
				modifier = Modifier.align(Alignment.End),
				horizontalArrangement = Arrangement.spacedBy(6.dp)
			) {
				TextButton(
					onClick = onCancel,
					enabled = !isRenaming
				) {
					Text(text = stringResource(id = R.string.action_cancel))
				}
				Button(
					onClick = onRename,
					enabled = !isRenaming,
					shape = MaterialTheme.shapes.large
				) {
					Text(text = stringResource(id = R.string.rename_recording_action))
				}
			}
		}
	}
}

@PreviewLightDark
@Composable
private fun RenameRecordingsDialogContentPreview() = RecorderAppTheme {
	RenameRecordingDialogContent(
		value = TextFieldValue(),
		onValueChange = {},
		onCancel = {},
	)
}