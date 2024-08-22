package com.eva.recorderapp.voice_recorder.presentation.recordings.composable

import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.data.recordings.files.RecordingsUtils
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.SelectableRecordings
import kotlinx.collections.immutable.ImmutableList

@Composable
fun TrashSelectedRecordingsButton(
	recordings: ImmutableList<SelectableRecordings>,
	onLegacyDelete: () -> Unit,
	modifier: Modifier = Modifier,
	shape: Shape = MaterialTheme.shapes.medium,
	containerColor: Color = FloatingActionButtonDefaults.containerColor,
	contentColor: Color = contentColorFor(containerColor),
	elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
) {
	val selectedModels by remember(recordings) {
		derivedStateOf {
			recordings.filter(SelectableRecordings::isSelected)
				.map(SelectableRecordings::recoding)
		}
	}

	var showDialog by remember { mutableStateOf(false) }

	val context = LocalContext.current

	val launcher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.StartIntentSenderForResult(),
		onResult = { result ->

			val message = if (result.resultCode == Activity.RESULT_OK)
				context.getString(R.string.recording_trash_request_success)
			else context.getString(R.string.recording_trash_request_falied)

			Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
		}
	)

	val onButtonClick: () -> Unit = remember(selectedModels) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			{
				val intentSender = RecordingsUtils.createTrashRequest(
					context = context,
					models = selectedModels
				)
				launcher.launch(intentSender)
			}
		} else onLegacyDelete
	}

	if (showDialog)
		AlertDialog(
			onDismissRequest = { showDialog = false },
			confirmButton = {
				TextButton(
					onClick = {
						onButtonClick()
						showDialog = false
					},
				) {
					Text(text = stringResource(id = R.string.recording_action_delete))
				}
			},
			dismissButton = {
				TextButton(onClick = { showDialog = false }) {
					Text(text = stringResource(id = R.string.action_cancel))
				}
			},
			title = { Text(text = stringResource(id = R.string.recording_trash_dialog_title)) },
			text = { Text(text = stringResource(id = R.string.recording_trash_dialog_text)) },
			icon = {
				Icon(
					painter = painterResource(id = R.drawable.ic_eraser),
					contentDescription = stringResource(id = R.string.recording_action_trash)
				)
			},
		)

	ExtendedFloatingActionButton(
		onClick = { showDialog = true },
		modifier = modifier,
		shape = shape,
		contentColor = contentColor,
		containerColor = containerColor,
		elevation = elevation
	) {
		Icon(
			painter = painterResource(id = R.drawable.ic_eraser),
			contentDescription = stringResource(id = R.string.recording_action_trash)
		)
		Spacer(modifier = Modifier.width(6.dp))
		Text(text = stringResource(id = R.string.recording_action_trash))
	}
}