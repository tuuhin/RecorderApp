package com.eva.feature_settings.composables.files

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme

@Composable
internal fun ArtistNameSelector(
	artistName: String,
	onNameChanged: (String) -> Unit,
	modifier: Modifier = Modifier
) {

	var showDialog by rememberSaveable { mutableStateOf(false) }

	ArtistNameSelectorDialog(
		initialValue = artistName,
		showDialog = showDialog,
		onDismiss = { showDialog = false },
		onRename = {
			onNameChanged(it)
			showDialog = false
		}
	)

	ListItem(
		headlineContent = {
			Text(
				text = stringResource(id = R.string.recordings_artist_name_settings_title),
				style = MaterialTheme.typography.titleMedium
			)
		},
		leadingContent = {
			Icon(
				painter = painterResource(R.drawable.ic_music_artist),
				contentDescription = "Update artist name"
			)
		},
		supportingContent = { Text(text = artistName) },
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
private fun ArtistNameSelectorDialog(
	showDialog: Boolean,
	onDismiss: () -> Unit,
	onRename: (String) -> Unit,
	modifier: Modifier = Modifier,
	initialValue: String = "",
	properties: DialogProperties = DialogProperties()
) {
	if (!showDialog) return

	val textFieldState = rememberTextFieldState(initialText = initialValue)

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
					text = stringResource(id = R.string.recordings_artist_name_settings_title),
					style = MaterialTheme.typography.headlineSmall,
					color = AlertDialogDefaults.titleContentColor,
					modifier = Modifier.padding(12.dp)
				)
				Text(
					text = stringResource(id = R.string.recordings_artist_name_settings_text),
					color = AlertDialogDefaults.textContentColor,
					style = MaterialTheme.typography.bodyMedium,
					modifier = Modifier.padding(vertical = 6.dp),
				)
				OutlinedTextField(
					state = textFieldState,
					shape = MaterialTheme.shapes.large,
					onKeyboardAction = {
						onRename(textFieldState.text.toString())
						it()
					},
					keyboardOptions = KeyboardOptions(
						keyboardType = KeyboardType.Text,
						imeAction = ImeAction.Done
					)
				)
				AnimatedVisibility(
					visible = textFieldState.text.isNotBlank(),
					enter = slideInVertically(),
					exit = slideOutVertically(),
					modifier = Modifier.fillMaxWidth(),
				) {
					Text(
						text = stringResource(R.string.recordings_artist_name_clear_warning_text),
						color = MaterialTheme.colorScheme.secondary,
						style = MaterialTheme.typography.labelMedium
					)
				}
				Spacer(modifier = Modifier.height(8.dp))
				Row(
					modifier = Modifier.align(Alignment.End),
					horizontalArrangement = Arrangement.spacedBy(6.dp)
				) {
					TextButton(onClick = onDismiss) {
						Text(text = stringResource(id = R.string.action_cancel))
					}
					Button(
						onClick = { onRename(textFieldState.text.toString()) },
					) {
						Text(text = stringResource(id = R.string.action_set))
					}
				}
			}
		}
	}
}

@PreviewLightDark
@Composable
private fun ArtistNameSelectorDialogPreview() = RecorderAppTheme {
	ArtistNameSelectorDialog(
		showDialog = true,
		onDismiss = {},
		onRename = {},
	)
}