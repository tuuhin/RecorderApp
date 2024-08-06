package com.eva.recorderapp.voice_recorder.presentation.record_player.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.player.PlayerPlayBackSpeed
import com.eva.recorderapp.voice_recorder.presentation.composables.IconButtonWithText

@Composable
fun AudioPlayerActions(
	isPlaying: Boolean,
	onPause: () -> Unit,
	onPlay: () -> Unit,
	modifier: Modifier = Modifier,
	speed: PlayerPlayBackSpeed = PlayerPlayBackSpeed.NORMAL,
	canRepeat: Boolean = false,
	onRepeatModeChange: (Boolean) -> Unit = {},
	onMutePlayer: () -> Unit = {},
	onSpeedChange: () -> Unit = {},
	onForward: () -> Unit = {},
	onRewind: () -> Unit = {},
	shape: Shape = MaterialTheme.shapes.large,
	color: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
	contentColor: Color = contentColorFor(backgroundColor = color)
) {

	Surface(
		modifier = modifier,
		shape = shape,
		color = color,
		contentColor = contentColor
	) {
		Column(
			modifier = Modifier.padding(all = dimensionResource(id = R.dimen.player_options_card_padding)),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(24.dp)
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				IconButtonWithText(
					icon = {
						Icon(
							painter = painterResource(id = R.drawable.ic_mute_device),
							contentDescription = stringResource(id = R.string.player_action_mute)
						)
					},
					text = stringResource(id = R.string.player_action_mute),
					onClick = onMutePlayer,
				)
				IconButtonWithText(
					icon = {
						Icon(
							painter = painterResource(id = R.drawable.ic_repeat),
							contentDescription = stringResource(id = R.string.player_action_repeat),
							tint = if (canRepeat) MaterialTheme.colorScheme.primary else contentColor
						)
					},
					text = stringResource(id = R.string.player_action_repeat),
					onClick = { onRepeatModeChange(!canRepeat) },
				)
				IconButtonWithText(
					icon = {
						Text(
							text = stringResource(R.string.player_playback_speed, speed.speed),
							style = MaterialTheme.typography.titleMedium,
							color = MaterialTheme.colorScheme.onSurface
						)
					},
					text = stringResource(id = R.string.player_action_speed),
					onClick = onSpeedChange,
				)
			}
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				IconButtonWithText(
					icon = {
						Icon(
							painter = painterResource(id = R.drawable.ic_fast_rewind),
							contentDescription = stringResource(id = R.string.player_fast_rewind)
						)
					},
					text = stringResource(id = R.string.player_fast_rewind),
					onClick = onRewind,
				)
				FloatingActionButton(
					onClick = {
						if (isPlaying) onPause()
						else onPlay()
					},
					elevation = FloatingActionButtonDefaults.loweredElevation(),
					contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
					containerColor = MaterialTheme.colorScheme.secondaryContainer
				) {
					AnimatedContent(
						targetState = isPlaying,
						transitionSpec = { isPlayingAnimation() },
						label = "Trasform between playing states",
						contentAlignment = Alignment.Center
					) { playing ->
						if (playing)
							Icon(
								painter = painterResource(id = R.drawable.ic_pause),
								contentDescription = stringResource(R.string.recorder_action_pause)
							)
						else Icon(
							painter = painterResource(id = R.drawable.ic_play),
							contentDescription = stringResource(R.string.recorder_action_resume)
						)
					}
				}
				IconButtonWithText(
					icon = {
						Icon(
							painter = painterResource(id = R.drawable.ic_fast_forward),
							contentDescription = stringResource(id = R.string.player_fast_forward)
						)
					},
					text = stringResource(id = R.string.player_fast_forward),
					onClick = onForward,
				)
			}
		}
	}
}

private fun AnimatedContentTransitionScope<Boolean>.isPlayingAnimation(): ContentTransform {
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
	)
}

@PreviewLightDark
@Composable
private fun AudioPlayerActionsPreview() = RecorderAppTheme {
	AudioPlayerActions(
		speed = PlayerPlayBackSpeed.NORMAL,
		isPlaying = true,
		onPlay = {},
		onPause = {})
}