package com.eva.recorderapp.voice_recorder.presentation.recordings.composable

import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.data.files.RecordingsUtils
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.SelectableTrashRecordings
import kotlinx.collections.immutable.ImmutableList

@Composable
fun DeleteRecordingsButton(
	recordings: ImmutableList<SelectableTrashRecordings>,
	onDelete: () -> Unit,
	modifier: Modifier = Modifier,
	shape: Shape = FloatingActionButtonDefaults.shape,
	containerColor: Color = FloatingActionButtonDefaults.containerColor,
	contentColor: Color = contentColorFor(containerColor),
	elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
) {
	val selectedModels by remember {
		derivedStateOf {
			recordings.filter(SelectableTrashRecordings::isSelected)
				.map(SelectableTrashRecordings::trashRecording)
		}
	}

	var showDialog by remember { mutableStateOf(false) }

	val context = LocalContext.current

	val launcher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.StartIntentSenderForResult(),
		onResult = { result ->

			val message = if (result.resultCode == Activity.RESULT_OK)
				context.getString(R.string.recording_delete_request_success)
			else context.getString(R.string.recording_delete_request_falied)

			Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
		}
	)

	val onButtonClick: () -> Unit = remember(context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			{
				val intentSender = RecordingsUtils.createDeleteRequest(
					context = context,
					models = selectedModels
				)
				launcher.launch(intentSender)

			}
		} else onDelete
	}

	if (showDialog)
		AlertDialog(
			onDismissRequest = { showDialog = false },
			confirmButton = {
				TextButton(onClick = onButtonClick) {
					Text(text = stringResource(id = R.string.recording_action_delete))
				}
			},
			dismissButton = {
				TextButton(onClick = { showDialog = false }) {
					Text(text = stringResource(id = R.string.action_cancel))
				}
			},
			title = {
				Text(text = stringResource(id = R.string.recording_permanent_delete_dialog_title))
			},
			text = {
				Text(text = stringResource(id = R.string.recording_permanent_delete_dialog_text))
			},
			icon = {
				Icon(
					imageVector = Icons.Outlined.Delete,
					contentDescription = stringResource(R.string.recording_action_delete)
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
			imageVector = Icons.Outlined.Delete,
			contentDescription = stringResource(id = R.string.recording_action_delete)
		)
		Spacer(modifier = Modifier.width(6.dp))
		Text(text = stringResource(id = R.string.recording_action_delete))
	}
}