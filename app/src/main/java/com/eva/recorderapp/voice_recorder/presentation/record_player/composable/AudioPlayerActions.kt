package com.eva.recorderapp.voice_recorder.presentation.record_player.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.player.PlayerMetaData
import com.eva.recorderapp.voice_recorder.domain.player.PlayerState
import com.eva.recorderapp.voice_recorder.presentation.composables.IconButtonWithText

@Composable
fun AudioPlayerActions(
	playerMetaData: PlayerMetaData,
	onPause: () -> Unit,
	onPlay: () -> Unit,
	modifier: Modifier = Modifier,
	onRepeatModeChange: (Boolean) -> Unit = {},
	onMutePlayer: () -> Unit = {},
	onSpeedChange: () -> Unit = {},
	onForward: () -> Unit = {},
	onRewind: () -> Unit = {},
	shape: Shape = MaterialTheme.shapes.large,
	color: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
	contentColor: Color = contentColorFor(backgroundColor = color),
	shadowElevation: Dp = 0.dp,
) {
	Surface(
		modifier = modifier,
		shape = shape,
		color = color,
		contentColor = contentColor,
		shadowElevation = shadowElevation
	) {
		Column(
			modifier = Modifier
				.padding(all = dimensionResource(id = R.dimen.player_actions_padding)),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(16.dp)
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				IconButtonWithText(
					icon = {
						Icon(
							painter = painterResource(id = R.drawable.ic_mute_device),
							contentDescription = stringResource(id = R.string.player_action_mute),
							tint = if (playerMetaData.isMuted) MaterialTheme.colorScheme.primary else contentColor
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
							tint = if (playerMetaData.isRepeating) MaterialTheme.colorScheme.primary else contentColor
						)
					},
					text = stringResource(id = R.string.player_action_repeat),
					onClick = { onRepeatModeChange(!playerMetaData.isRepeating) },
				)
				IconButtonWithText(
					icon = {
						Text(
							text = stringResource(
								R.string.player_playback_speed,
								playerMetaData.playBackSpeed.speed
							),
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
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
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
				AnimatedPlayPauseButton(
					isPlaying = playerMetaData.isPlaying,
					onPause = onPause,
					onPlay = onPlay
				)
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


@PreviewLightDark
@Composable
private fun AudioPlayerActionsPreview() = RecorderAppTheme {
	AudioPlayerActions(
		playerMetaData = PlayerMetaData(playerState = PlayerState.PLAYER_READY),
		onPlay = {},
		onPause = {},
	)
}