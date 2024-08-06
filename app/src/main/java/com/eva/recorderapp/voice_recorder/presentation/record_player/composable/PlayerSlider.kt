package com.eva.recorderapp.voice_recorder.presentation.record_player.composable

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.voice_recorder.domain.player.PlayerTrackData
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PlayerSlider(
	track: PlayerTrackData,
	onSeekToDuration: (Duration) -> Unit,
	modifier: Modifier = Modifier,
	onSeekDurationComplete: () -> Unit = {},
) {

	val sliderPercentage by remember(track.current) {
		derivedStateOf(track::playRatio)
	}

	Slider(
		value = sliderPercentage,
		onValueChange = { seekAmt ->
			if (track.total.isPositive()) {
				val seekAmount = (track.total.inWholeMilliseconds * seekAmt).toLong()
				val amt = seekAmount.coerceIn(0L, track.total.inWholeMilliseconds)
				val duration = amt.milliseconds
				onSeekToDuration(duration)
			}
		},
		onValueChangeFinished = onSeekDurationComplete,
		colors = SliderDefaults.colors(
			activeTrackColor = MaterialTheme.colorScheme.secondary,
			thumbColor = MaterialTheme.colorScheme.secondary,
			inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
		),
		modifier = modifier.padding(horizontal = 8.dp),
	)

}
