package com.eva.feature_recorder.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.eva.datastore.domain.models.TranscriptionSettings
import com.eva.recorder.domain.models.RecorderState
import com.eva.ui.R

@Composable
internal fun AssociatedRecorderActions(
	onAddBookMark: () -> Unit,
	onOpenSettings: () -> Unit,
	modifier: Modifier = Modifier,
	recorderState: RecorderState = RecorderState.RECORDING,
	transcriptionSettings: TranscriptionSettings = TranscriptionSettings(),
) {

	var showDialog by rememberSaveable { mutableStateOf(false) }

	TranscriptionsDialog(
		showDialog = showDialog,
		onOpenSettings = {
			onOpenSettings()
			showDialog = false
		},
		transcriptionSettings = transcriptionSettings,
		onDismiss = { showDialog = false },
		iconContentColor = MaterialTheme.colorScheme.primary,
	)

	Row(
		modifier = modifier,
		horizontalArrangement = Arrangement.spacedBy(12.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		SuggestionChip(
			onClick = { showDialog = true },
			icon = {
				Icon(
					painter = painterResource(R.drawable.ic_info),
					contentDescription = null,
					modifier = Modifier.size(20.dp),
				)
			},
			border = null,
			label = { Text(text = stringResource(R.string.app_settings_transcriptions)) },
			shape = MaterialTheme.shapes.extraLarge,
			colors = SuggestionChipDefaults.suggestionChipColors(
				containerColor = MaterialTheme.colorScheme.tertiary,
				labelColor = MaterialTheme.colorScheme.onTertiary,
				iconContentColor = MaterialTheme.colorScheme.onTertiary
			)
		)
		SuggestionChip(
			onClick = onAddBookMark,
			icon = {
				Icon(
					painter = painterResource(R.drawable.ic_boomark_add),
					contentDescription = null,
					modifier = Modifier.size(20.dp),
				)
			},
			border = null,
			label = { Text(stringResource(R.string.add_recording_bookmark)) },
			enabled = recorderState == RecorderState.RECORDING,
			shape = MaterialTheme.shapes.extraLarge,
			colors = SuggestionChipDefaults.suggestionChipColors(
				containerColor = MaterialTheme.colorScheme.secondary,
				labelColor = MaterialTheme.colorScheme.onSecondary,
				iconContentColor = MaterialTheme.colorScheme.onSecondary
			)
		)
	}
}


@Composable
private fun TranscriptionsDialog(
	showDialog: Boolean,
	onDismiss: () -> Unit,
	onOpenSettings: () -> Unit,
	modifier: Modifier = Modifier,
	transcriptionSettings: TranscriptionSettings = TranscriptionSettings(),
	shape: Shape = AlertDialogDefaults.shape,
	tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
	containerColor: Color = AlertDialogDefaults.containerColor,
	titleContentColor: Color = AlertDialogDefaults.titleContentColor,
	textContentColor: Color = AlertDialogDefaults.textContentColor,
	iconContentColor: Color = AlertDialogDefaults.iconContentColor,
	properties: DialogProperties = DialogProperties()
) {

	if (!showDialog) return

	AlertDialog(
		onDismissRequest = onDismiss,
		confirmButton = {
			AnimatedVisibility(visible = !transcriptionSettings.isEnabled || transcriptionSettings.modelId == null) {
				Button(
					onClick = onOpenSettings,
					colors = ButtonDefaults.buttonColors(
						containerColor = MaterialTheme.colorScheme.primaryContainer,
						contentColor = MaterialTheme.colorScheme.onPrimaryContainer
					)
				) {
					Text("Open Settings")
				}
			}
		},
		title = { Text(text = stringResource(R.string.recorder_transcriptions_about_dialog_title)) },
		text = {
			if (transcriptionSettings.isEnabled && transcriptionSettings.modelId != null)
				Text(
					text = buildAnnotatedString {
						append(stringResource(R.string.recorder_transcriptions_about_dialog_text_enabled))
						withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
							append(" ${transcriptionSettings.modelId}")
						}
					}
				)
			else Text(stringResource(R.string.recorder_transcriptions_about_dialog_text_not_enabled))
		},
		icon = {
			Icon(
				painter = painterResource(R.drawable.ic_stt),
				contentDescription = null, modifier = Modifier.size(32.dp),
			)
		},
		modifier = modifier,
		shape = shape,
		tonalElevation = tonalElevation,
		containerColor = containerColor,
		textContentColor = textContentColor,
		titleContentColor = titleContentColor,
		iconContentColor = iconContentColor,
		properties = properties
	)
}