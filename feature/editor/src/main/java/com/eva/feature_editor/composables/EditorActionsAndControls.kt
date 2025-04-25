package com.eva.feature_editor.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.eva.feature_editor.event.EditorScreenEvent
import com.eva.player.domain.model.PlayerTrackData
import com.eva.player_shared.composables.AnimatedPlayPauseButton
import com.eva.ui.R

@Composable
private fun EditorActionsAndControls(
	trackData: PlayerTrackData,
	isMediaPlaying: Boolean,
	modifier: Modifier = Modifier,
	onTrimMedia: () -> Unit,
	onPlay: () -> Unit,
	onPause: () -> Unit,
) {
	val sliderPercentage by remember(trackData.current) {
		derivedStateOf(trackData::playRatio)
	}

	Column(
		modifier = modifier,
		verticalArrangement = Arrangement.spacedBy(40.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Slider(
			value = sliderPercentage,
			onValueChange = { seekAmt -> },
			onValueChangeFinished = { },
			colors = SliderDefaults.colors(
				activeTrackColor = MaterialTheme.colorScheme.primary,
				thumbColor = MaterialTheme.colorScheme.primary,
				inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
			),
		)
		//actions
		Row(
			horizontalArrangement = Arrangement.spacedBy(40.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			// cut option
			SmallFloatingActionButton(
				onClick = onTrimMedia,
				shape = CircleShape,
				containerColor = MaterialTheme.colorScheme.tertiaryContainer,
				contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
				elevation = FloatingActionButtonDefaults.loweredElevation()
			) {
				Icon(
					painter = painterResource(R.drawable.ic_cut),
					contentDescription = "Action Cut"
				)
			}
			AnimatedPlayPauseButton(
				isPlaying = isMediaPlaying,
				onPlay = onPlay,
				onPause = onPause,
				tonalElevation = 1.dp,
				shadowElevation = 1.dp
			)
			// add other options if needed
		}
	}
}

@Composable
fun EditorActionsAndControls(
	trackData: PlayerTrackData,
	isMediaPlaying: Boolean,
	onEvent: (EditorScreenEvent) -> Unit,
	modifier: Modifier = Modifier,
) {
	EditorActionsAndControls(
		isMediaPlaying = isMediaPlaying,
		trackData = trackData,
		modifier = modifier,
		onPlay = { onEvent(EditorScreenEvent.PlayAudio) },
		onPause = { EditorScreenEvent.PauseAudio },
		onTrimMedia = {},
	)
}