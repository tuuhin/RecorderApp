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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.data.files.RecordingsUtils
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.SelectableRecordings
import kotlinx.collections.immutable.ImmutableList

@Composable
fun TrashSelectedRecordingsButton(
	recordings: ImmutableList<SelectableRecordings>,
	onDelete: () -> Unit,
	modifier: Modifier = Modifier,
	shape: Shape = FloatingActionButtonDefaults.shape,
	containerColor: Color = FloatingActionButtonDefaults.containerColor,
	contentColor: Color = contentColorFor(containerColor),
	elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
) {
	val selectedModels by remember {
		derivedStateOf {
			recordings.filter(SelectableRecordings::isSelected)
				.map(SelectableRecordings::recoding)
		}
	}

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

	val onButtonClick: () -> Unit = remember(context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			{
				val intentSender = RecordingsUtils.createTrashRequest(
					context = context,
					models = selectedModels
				)
				launcher.launch(intentSender)

			}
		} else onDelete
	}

	ExtendedFloatingActionButton(
		onClick = onButtonClick,
		modifier = modifier,
		shape = shape,
		contentColor = contentColor,
		containerColor = containerColor,
		elevation = elevation
	) {
		Icon(
			imageVector = Icons.Outlined.Delete,
			contentDescription = stringResource(id = R.string.recording_action_trash)
		)
		Spacer(modifier = Modifier.width(6.dp))
		Text(text = stringResource(id = R.string.recording_action_trash))
	}
}