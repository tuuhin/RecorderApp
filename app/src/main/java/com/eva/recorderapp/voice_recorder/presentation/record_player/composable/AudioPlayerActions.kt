package com.eva.recorderapp.voice_recorder.presentation.record_player.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.eva.recorderapp.voice_recorder.domain.player.model.PlayerMetaData
import com.eva.recorderapp.voice_recorder.domain.player.model.PlayerPlayBackSpeed
import com.eva.recorderapp.voice_recorder.domain.player.model.PlayerState
import com.eva.recorderapp.voice_recorder.presentation.composables.IconButtonWithText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerActions(
	playerMetaData: PlayerMetaData,
	onPause: () -> Unit,
	onPlay: () -> Unit,
	modifier: Modifier = Modifier,
	onRepeatModeChange: (Boolean) -> Unit = {},
	onMutePlayer: () -> Unit = {},
	onForward: () -> Unit = {},
	onRewind: () -> Unit = {},
	onSpeedSelected: (PlayerPlayBackSpeed) -> Unit = {},
	shape: Shape = MaterialTheme.shapes.large,
	color: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
	contentColor: Color = contentColorFor(backgroundColor = color),
	iconActiveColor: Color = MaterialTheme.colorScheme.primary,
) {
	val playBackSpeedBottomSheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
	val scope = rememberCoroutineScope()

	var openPlayBackSpeedBottomSheet by remember { mutableStateOf(false) }

	if (openPlayBackSpeedBottomSheet) {
		ModalBottomSheet(
			sheetState = playBackSpeedBottomSheet,
			onDismissRequest = { openPlayBackSpeedBottomSheet = false },
		) {
			PlayBackSpeedSelector(
				selectedSpeed = playerMetaData.playBackSpeed,
				onSpeedSelected = onSpeedSelected,
				contentPadding = PaddingValues(all = dimensionResource(id = R.dimen.bottom_sheet_padding_lg))
			)
		}
	}

	Surface(
		modifier = modifier,
		shape = shape,
		color = color,
		contentColor = contentColor,
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
							tint = if (playerMetaData.isMuted) iconActiveColor else contentColor
						)
					},
					enabled = false,
					text = stringResource(id = R.string.player_action_mute),
					onClick = onMutePlayer,
				)
				IconButtonWithText(
					icon = {
						Icon(
							painter = painterResource(id = R.drawable.ic_repeat),
							contentDescription = stringResource(id = R.string.player_action_repeat),
							tint = if (playerMetaData.isRepeating) iconActiveColor else contentColor
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
					onClick = {
						scope.launch { playBackSpeedBottomSheet.show() }
							.invokeOnCompletion { openPlayBackSpeedBottomSheet = true }
					},
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