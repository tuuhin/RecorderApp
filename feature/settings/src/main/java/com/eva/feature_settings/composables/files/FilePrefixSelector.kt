package com.eva.feature_settings.composables.files

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme

@Composable
internal fun FilePrefixSelector(
	prefix: String,
	onPrefixChange: (String) -> Unit,
	modifier: Modifier = Modifier,
) {

	var showDialog by remember {
		mutableStateOf(false)
	}

	var textValue by remember {
		mutableStateOf(TextFieldValue())
	}

	RenamePrefixDialog(
		showDialog = showDialog,
		textFieldValue = textValue,
		onTextFieldValueChange = { textValue = it },
		onDismiss = { showDialog = false },
		onRename = {
			onPrefixChange(it.text)
			showDialog = false
		}
	)

	ListItem(
		headlineContent = {
			Text(
				text = stringResource(id = R.string.recording_settings_change_name_prefix),
				style = MaterialTheme.typography.titleMedium
			)
		},
		leadingContent = {
			Icon(
				painter = painterResource(R.drawable.ic_rename),
				contentDescription = stringResource(id = R.string.recording_settings_change_name_prefix)
			)
		},
		supportingContent = { Text(text = prefix) },
		modifier = modifier
			.clip(shape = MaterialTheme.shapes.medium)
			.clickable { showDialog = true },
		tonalElevation = 0.dp,
		shadowElevation = 0.dp,
		colors = ListItemDefaults.colors(containerColor = Color.Transparent)
	)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenamePrefixDialog(
	showDialog: Boolean,
	textFieldValue: TextFieldValue,
	onTextFieldValueChange: (TextFieldValue) -> Unit,
	onDismiss: () -> Unit,
	onRename: (TextFieldValue) -> Unit,
	modifier: Modifier = Modifier, properties: DialogProperties = DialogProperties(),
) {
	if (!showDialog) return

	val isRenameEnabled by remember(textFieldValue) {
		derivedStateOf {
			textFieldValue.text.isBlank()
		}
	}

	BasicAlertDialog(
		onDismissRequest = onDismiss,
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
					text = stringResource(id = R.string.rename_recording_prefix_title),
					style = MaterialTheme.typography.headlineSmall,
					color = AlertDialogDefaults.titleContentColor,
					modifier = Modifier.padding(12.dp)
				)
				Text(
					text = stringResource(id = R.string.rename_recording_prefix_text),
					color = AlertDialogDefaults.textContentColor,
					style = MaterialTheme.typography.bodyMedium,
					modifier = Modifier.padding(vertical = 6.dp),
				)
				OutlinedTextField(
					value = textFieldValue,
					onValueChange = onTextFieldValueChange,
					label = { Text(text = stringResource(id = R.string.rename_label_text)) },
					shape = MaterialTheme.shapes.large,
					keyboardActions = KeyboardActions(onDone = { onRename(textFieldValue) }),
					keyboardOptions = KeyboardOptions(
						keyboardType = KeyboardType.Text, imeAction = ImeAction.Done
					)
				)
				Spacer(modifier = Modifier.height(8.dp))
				Row(
					modifier = Modifier.align(Alignment.End),
					horizontalArrangement = Arrangement.spacedBy(6.dp)
				) {
					TextButton(onClick = onDismiss) {
						Text(text = stringResource(id = R.string.action_cancel))
					}
					Button(
						onClick = { onRename(textFieldValue) },
						enabled = !isRenameEnabled
					) {
						Text(text = stringResource(id = R.string.rename_recording_action))
					}
				}

			}
		}
	}
}

@PreviewLightDark
@Composable
private fun RenamePrefixDialogPreview() = RecorderAppTheme {
	RenamePrefixDialog(
		showDialog = true,
		textFieldValue = TextFieldValue(),
		onTextFieldValueChange = {},
		onDismiss = { },
		onRename = {}
	)
}