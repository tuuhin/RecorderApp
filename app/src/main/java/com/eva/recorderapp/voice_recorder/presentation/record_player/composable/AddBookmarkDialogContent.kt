package com.eva.recorderapp.voice_recorder.presentation.record_player.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme

@Composable
fun AddBookmarkDialogContent(
	isUpdate: Boolean,
	textFieldValue: TextFieldValue,
	onValueChange: (TextFieldValue) -> Unit,
	onDismiss: () -> Unit,
	onConfirm: () -> Unit,
	modifier: Modifier = Modifier,
) {
	Surface(
		shape = AlertDialogDefaults.shape,
		tonalElevation = AlertDialogDefaults.TonalElevation,
		color = AlertDialogDefaults.containerColor,
		contentColor = contentColorFor(AlertDialogDefaults.containerColor),
		modifier = modifier,
	) {
		Column(
			modifier = Modifier.padding(24.dp)
		) {

			val text = if (isUpdate) stringResource(R.string.update_bookmark_dialog_title)
			else stringResource(id = R.string.add_bookmark_dialog_title)

			Text(
				text = text,
				style = MaterialTheme.typography.titleLarge,
				color = AlertDialogDefaults.titleContentColor,
				modifier = Modifier.padding(12.dp)
			)
			OutlinedTextField(
				value = textFieldValue,
				onValueChange = onValueChange,
				placeholder = {
					Text(
						text = stringResource(R.string.option_text_field),
						style = MaterialTheme.typography.labelLarge
					)
				},
				shape = MaterialTheme.shapes.medium,
				textStyle = MaterialTheme.typography.labelLarge,
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
				modifier = Modifier.fillMaxWidth()
			)
			Spacer(modifier = Modifier.height(12.dp))
			Row(
				modifier = Modifier.align(Alignment.End),
				horizontalArrangement = Arrangement.spacedBy(6.dp)
			) {
				TextButton(
					onClick = onDismiss,
				) {
					Text(text = stringResource(id = R.string.action_cancel))
				}
				Button(
					onClick = onConfirm,
					shape = MaterialTheme.shapes.large,
				) {
					val buttonText = if (isUpdate) stringResource(R.string.menu_option_update)
					else stringResource(id = R.string.menu_option_create)
					Text(text = buttonText)
				}
			}
		}
	}
}

@PreviewLightDark
@Composable
private fun AddBookmarkDialogContentPreview() = RecorderAppTheme {
	AddBookmarkDialogContent(
		isUpdate = true,
		textFieldValue = TextFieldValue(),
		onValueChange = {},
		onConfirm = {},
		onDismiss = {})
}