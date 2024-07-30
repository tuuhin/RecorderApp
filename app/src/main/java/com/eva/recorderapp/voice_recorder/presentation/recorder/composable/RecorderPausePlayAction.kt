package com.eva.recorderapp.voice_recorder.presentation.recorder.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.domain.emums.RecorderState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecorderPausePlayAction(
	state: RecorderState,
	onResume: () -> Unit,
	onPause: () -> Unit,
	onCancel: () -> Unit,
	onStop: () -> Unit,
	modifier: Modifier = Modifier
) {
	Box(
		modifier = modifier,
		contentAlignment = Alignment.Center
	) {
		AnimatedContent(
			targetState = state == RecorderState.PAUSED,
			transitionSpec = {
				if (initialState) {
					slideInVertically { height -> height } togetherWith slideOutVertically { height -> -height }
				} else slideInVertically { height -> -height } togetherWith slideOutVertically { height -> height }
			},
			modifier = Modifier.align(Alignment.CenterStart),
		) { isPaused ->
			if (isPaused) {
				IconButton(
					onClick = onResume,
					colors = IconButtonDefaults.iconButtonColors(
						containerColor = MaterialTheme.colorScheme.tertiary,
						contentColor = MaterialTheme.colorScheme.onTertiary
					),
					modifier = Modifier.size(dimensionResource(id = R.dimen.recorder_button_size))
				) {
					Icon(
						painter = painterResource(id = R.drawable.ic_play),
						contentDescription = stringResource(id = R.string.action_pasued)
					)
				}
			} else IconButton(
				onClick = onPause,
				colors = IconButtonDefaults.iconButtonColors(
					containerColor = MaterialTheme.colorScheme.tertiary,
					contentColor = MaterialTheme.colorScheme.onTertiary
				),
				modifier = Modifier.size(dimensionResource(id = R.dimen.recorder_button_size))
			) {
				Icon(
					painter = painterResource(id = R.drawable.ic_pause),
					contentDescription = stringResource(id = R.string.action_pasued)
				)
			}
		}

		IconButton(
			onClick = onCancel,
			colors = IconButtonDefaults.iconButtonColors(
				containerColor = MaterialTheme.colorScheme.secondary,
				contentColor = MaterialTheme.colorScheme.onSecondary
			),
			modifier = Modifier
				.align(Alignment.TopCenter)
				.size(dimensionResource(id = R.dimen.recorder_button_size))
		) {
			Icon(
				painter = painterResource(id = R.drawable.ic_close),
				contentDescription = stringResource(id = R.string.recorder_action_cancel)
			)
		}

		IconButton(
			onClick = onStop,
			colors = IconButtonDefaults.iconButtonColors(
				containerColor = MaterialTheme.colorScheme.primary,
				contentColor = MaterialTheme.colorScheme.onPrimary
			),
			modifier = Modifier
				.align(Alignment.CenterEnd)
				.size(dimensionResource(id = R.dimen.recorder_button_size))
		) {
			Icon(
				painter = painterResource(id = R.drawable.ic_stop),
				contentDescription = stringResource(id = R.string.recorder_action_stop)
			)
		}
	}
}
