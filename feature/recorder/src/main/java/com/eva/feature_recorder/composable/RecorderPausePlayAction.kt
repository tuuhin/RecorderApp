package com.eva.feature_recorder.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.eva.ui.R


@Composable
internal fun RecorderPausePlayAction(
	showPausedAction: Boolean,
	onResume: () -> Unit,
	onPause: () -> Unit,
	onCancel: () -> Unit,
	onStop: () -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
) {

	var showCancelDialog by remember { mutableStateOf(false) }
	var showSaveDialog by remember { mutableStateOf(false) }


	CancelRecordingDialog(
		showDialog = showCancelDialog,
		onDismiss = { showCancelDialog = false },
		onDiscard = onCancel,
	)

	SaveRecordingDialog(
		showDialog = showSaveDialog,
		onDismiss = { showSaveDialog = false },
		onSave = onStop
	)

	Box(
		modifier = modifier,
		contentAlignment = Alignment.Center
	) {
		AnimatedContent(
			targetState = showPausedAction,
			transitionSpec = { recorderStateAnimation() },
			modifier = Modifier.align(Alignment.CenterStart),
			label = "Pause Play Action Animation",
		) { isPaused ->

			if (isPaused) {
				IconButton(
					onClick = onResume,
					enabled = enabled,
					colors = IconButtonDefaults.iconButtonColors(
						containerColor = MaterialTheme.colorScheme.tertiary,
						contentColor = MaterialTheme.colorScheme.onTertiary
					),
					modifier = Modifier.size(dimensionResource(id = R.dimen.recorder_button_size))
				) {
					Icon(
						painter = painterResource(id = R.drawable.ic_play),
						contentDescription = stringResource(id = R.string.action_paused)
					)
				}
			} else {
				IconButton(
					onClick = onPause,
					enabled = enabled,
					colors = IconButtonDefaults.iconButtonColors(
						containerColor = MaterialTheme.colorScheme.tertiary,
						contentColor = MaterialTheme.colorScheme.onTertiary
					),
					modifier = Modifier.size(dimensionResource(id = R.dimen.recorder_button_size))
				) {
					Icon(
						painter = painterResource(id = R.drawable.ic_pause),
						contentDescription = stringResource(id = R.string.action_paused)
					)
				}
			}
		}


		IconButton(
			onClick = {
				onPause()
				showCancelDialog = true
			},
			enabled = enabled,
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
			onClick = {
				onPause()
				showSaveDialog = true
			},
			enabled = enabled,
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

private fun AnimatedContentTransitionScope<Boolean>.recorderStateAnimation(): ContentTransform {
	return fadeIn(
		animationSpec = tween(400)
	) + scaleIn(
		animationSpec = spring(
			dampingRatio = Spring.DampingRatioLowBouncy,
			stiffness = Spring.StiffnessLow,
		),
	) togetherWith fadeOut(
		animationSpec = tween(400)
	) + scaleOut(
		animationSpec = spring(
			dampingRatio = Spring.DampingRatioLowBouncy,
			stiffness = Spring.StiffnessLow,
		),
	) using SizeTransform(clip = false)
}