package com.eva.feature_player.composable

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
import com.eva.player.domain.model.PlayerMetaData
import com.eva.player.domain.model.PlayerPlayBackSpeed
import com.eva.player.domain.model.PlayerState
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AudioPlayerActions(
	playerMetaData: PlayerMetaData,
	onPause: () -> Unit,
	onPlay: () -> Unit,
	modifier: Modifier = Modifier,
	onRepeatModeChange: (Boolean) -> Unit = {},
	onMuteStream: () -> Unit = {},
	onForward: () -> Unit = {},
	onRewind: () -> Unit = {},
	isControllerReady: Boolean = true,
	onSpeedSelected: (PlayerPlayBackSpeed) -> Unit = {},
	shape: Shape = MaterialTheme.shapes.large,
	color: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
	contentColor: Color = contentColorFor(backgroundColor = color),
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
							painter = painterResource(id = R.drawable.ic_mute_stream),
							contentDescription = stringResource(id = R.string.player_action_mute),
						)
					},
					isSelected = playerMetaData.isMuted,
					text = stringResource(id = R.string.player_action_mute),
					onClick = onMuteStream,
				)
				IconButtonWithText(
					icon = {
						Icon(
							painter = painterResource(id = R.drawable.ic_repeat),
							contentDescription = stringResource(id = R.string.player_action_repeat),
						)
					},
					isSelected = playerMetaData.isRepeating,
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
					enabled = isControllerReady,
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