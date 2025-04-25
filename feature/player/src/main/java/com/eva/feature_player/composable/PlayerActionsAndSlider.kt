package com.eva.feature_player.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.feature_player.state.PlayerEvents
import com.eva.player.domain.model.PlayerMetaData
import com.eva.player.domain.model.PlayerTrackData
import com.eva.ui.theme.RecorderAppTheme

@Composable
internal fun PlayerActionsAndSlider(
	metaData: PlayerMetaData,
	trackData: PlayerTrackData,
	onPlayerAction: (PlayerEvents) -> Unit,
	modifier: Modifier = Modifier,
	isControllerSet: Boolean = true,
	containerShape: Shape = MaterialTheme.shapes.large,
	containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
	contentColor: Color = contentColorFor(backgroundColor = containerColor),
) {

	val sliderPercentage by remember(trackData.current) {
		derivedStateOf(trackData::playRatio)
	}

	Column(
		modifier = modifier,
		verticalArrangement = Arrangement.spacedBy(8.dp)
	) {
		Slider(
			value = sliderPercentage,
			onValueChange = { seekAmt ->
				val seek = trackData.calculateSeekAmount(seekAmt)
				onPlayerAction(PlayerEvents.OnSeekPlayer(seek))
			},
			onValueChangeFinished = { onPlayerAction(PlayerEvents.OnSeekComplete) },
			colors = SliderDefaults.colors(
				activeTrackColor = MaterialTheme.colorScheme.primary,
				thumbColor = MaterialTheme.colorScheme.primary,
				inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
			),
			enabled = isControllerSet,
		)
		AudioPlayerActions(
			playerMetaData = metaData,
			isControllerReady = isControllerSet,
			onPlay = { onPlayerAction(PlayerEvents.OnStartPlayer) },
			onPause = { onPlayerAction(PlayerEvents.OnPausePlayer) },
			onMuteStream = { onPlayerAction(PlayerEvents.OnMutePlayer) },
			onRepeatModeChange = { onPlayerAction(PlayerEvents.OnRepeatModeChange(it)) },
			onRewind = { onPlayerAction(PlayerEvents.OnRewindByNDuration()) },
			onForward = { onPlayerAction(PlayerEvents.OnForwardByNDuration()) },
			onSpeedSelected = { onPlayerAction(PlayerEvents.OnPlayerSpeedChange(it)) },
			color = containerColor,
			shape = containerShape,
			contentColor = contentColor,
		)
	}
}

@PreviewLightDark
@Composable
private fun PlayerActionsAndSliderPreview() = RecorderAppTheme {
	Surface {
		PlayerActionsAndSlider(
			metaData = PlayerMetaData(isPlaying = true),
			trackData = PlayerTrackData(),
			onPlayerAction = {},
			modifier = Modifier.padding(12.dp)
		)
	}
}