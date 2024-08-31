package com.eva.recorderapp.voice_recorder.presentation.record_player.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.player.PlayerMetaData
import com.eva.recorderapp.voice_recorder.domain.player.PlayerTrackData
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.PlayerEvents
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerActionsAndSlider(
	metaData: PlayerMetaData,
	trackData: PlayerTrackData,
	onPlayerAction: (PlayerEvents) -> Unit,
	modifier: Modifier = Modifier,
	containerShape: Shape = MaterialTheme.shapes.large,
	containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
	containerBackground: Color = contentColorFor(backgroundColor = containerColor)
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
				selectedSpeed = metaData.playBackSpeed,
				onSpeedSelected = { speed ->
					onPlayerAction(PlayerEvents.OnPlayerSpeedChange(speed))
				},
				contentPadding = PaddingValues(all = dimensionResource(id = R.dimen.bottom_sheet_padding_lg))
			)
		}
	}

	Column(
		modifier = modifier,
		verticalArrangement = Arrangement.spacedBy(8.dp)
	) {
		PlayerSlider(
			track = trackData,
			onSeekToDuration = { amount ->
				onPlayerAction(PlayerEvents.OnSeekPlayer(amount))
			},
			onSeekDurationComplete = { onPlayerAction(PlayerEvents.OnSeekComplete) },
		)
		AudioPlayerActions(
			playerMetaData = metaData,
			onPlay = { onPlayerAction(PlayerEvents.OnStartPlayer) },
			onPause = { onPlayerAction(PlayerEvents.OnPausePlayer) },
			onMutePlayer = { onPlayerAction(PlayerEvents.OnMutePlayer) },
			onRepeatModeChange = { onPlayerAction(PlayerEvents.OnRepeatModeChange(it)) },
			onRewind = { onPlayerAction(PlayerEvents.OnRewindByNDuration()) },
			onForward = { onPlayerAction(PlayerEvents.OnForwardByNDuration()) },
			onSpeedChange = {
				scope.launch { playBackSpeedBottomSheet.show() }
					.invokeOnCompletion { openPlayBackSpeedBottomSheet = true }
			},
			color = containerColor,
			shape = containerShape,
			contentColor = containerBackground,
		)
	}
}

@PreviewLightDark
@Composable
private fun PlayerActionsnAndSliderPreview() = RecorderAppTheme {
	Surface {
		PlayerActionsAndSlider(
			metaData = PreviewFakes.FAKE_AUDIO_INFORMATION.playerMetaData,
			trackData = PlayerTrackData(),
			onPlayerAction = {}, modifier = Modifier.padding(12.dp)
		)
	}
}