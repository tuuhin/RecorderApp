package com.eva.recorderapp.voice_recorder.presentation.recorder.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.domain.emums.RecorderAction
import com.eva.recorderapp.voice_recorder.domain.emums.RecorderState

@Composable
fun RecorderActionTray(
	recorderState: RecorderState,
	onRecorderAction: (RecorderAction) -> Unit,
	modifier: Modifier = Modifier
) {

	when (recorderState) {
		RecorderState.IDLE, RecorderState.COMPLETED -> Box(
			modifier = modifier,
			contentAlignment = Alignment.Center
		) {
			RecordButton(
				onClick = { onRecorderAction(RecorderAction.START_RECORDER) },
			)
		}

		RecorderState.RECORDING -> Row(
			modifier = modifier,
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			IconButton(
				onClick = { onRecorderAction(RecorderAction.PAUSE_RECORDER) },
				colors = IconButtonDefaults.iconButtonColors(
					containerColor = MaterialTheme.colorScheme.primary,
					contentColor = MaterialTheme.colorScheme.onPrimary
				)
			) {
				Icon(
					imageVector = Icons.Outlined.Pause,
					contentDescription = stringResource(id = R.string.action_pasued)
				)
			}
			IconButton(
				onClick = { onRecorderAction(RecorderAction.STOP_RECORDER) },
				colors = IconButtonDefaults.iconButtonColors(
					containerColor = MaterialTheme.colorScheme.primary,
					contentColor = MaterialTheme.colorScheme.onPrimary
				)
			) {
				Icon(
					imageVector = Icons.Outlined.Stop,
					contentDescription = stringResource(id = R.string.action_stop)
				)
			}
		}

		RecorderState.PAUSED -> Row(
			modifier = modifier,
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			IconButton(
				onClick = { onRecorderAction(RecorderAction.RESUME_RECORDER) },
				colors = IconButtonDefaults.iconButtonColors(
					containerColor = MaterialTheme.colorScheme.primary,
					contentColor = MaterialTheme.colorScheme.onPrimary
				)
			) {
				Icon(
					imageVector = Icons.Outlined.PlayArrow,
					contentDescription = stringResource(id = R.string.action_pasued)
				)
			}
			IconButton(
				onClick = { onRecorderAction(RecorderAction.STOP_RECORDER) },
				colors = IconButtonDefaults.iconButtonColors(
					containerColor = MaterialTheme.colorScheme.primary,
					contentColor = MaterialTheme.colorScheme.onPrimary
				)
			) {
				Icon(
					imageVector = Icons.Outlined.Stop,
					contentDescription = stringResource(id = R.string.action_stop)
				)
			}
		}
	}
}