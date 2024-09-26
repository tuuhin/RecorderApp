package com.eva.recorderapp.voice_recorder.presentation.recordings.composable

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.eva.recorderapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreRecordingsButton(
	onItemRestore: () -> Unit,
	modifier: Modifier = Modifier,
	colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
) {

	var showDialog by remember { mutableStateOf(false) }

	if (showDialog)
		AlertDialog(
			onDismissRequest = { showDialog = false },
			confirmButton = {
				TextButton(onClick = onItemRestore) {
					Text(text = stringResource(id = R.string.recording_action_restore))
				}
			},
			dismissButton = {
				TextButton(onClick = { showDialog = false }) {
					Text(text = stringResource(id = R.string.action_cancel))
				}
			},
			title = {
				Text(text = stringResource(id = R.string.recording_restore_dialog_title))
			},
			text = {
				Text(text = stringResource(id = R.string.recording_restore_dialog_text))
			},
			icon = {
				Icon(
					painter = painterResource(id = R.drawable.ic_restore),
					stringResource(id = R.string.recordings_restore_action)
				)
			},
		)

	TooltipBox(
		positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
		tooltip = {
			PlainTooltip {
				Text(text = stringResource(id = R.string.recordings_restore_action))
			}
		},
		state = rememberTooltipState()
	) {
		IconButton(
			onClick = { showDialog = true },
			modifier = modifier,
			colors = colors,
		) {
			Icon(
				painter = painterResource(R.drawable.ic_restore_simple),
				contentDescription = stringResource(id = R.string.recordings_restore_action)
			)
		}
	}
}
