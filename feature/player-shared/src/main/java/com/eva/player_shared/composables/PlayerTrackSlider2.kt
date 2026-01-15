package com.eva.player_shared.composables

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.eva.player.domain.model.PlayerTrackData
import com.eva.ui.theme.RecorderAppTheme
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerTrackSlider2(
	trackData: () -> PlayerTrackData,
	onSeek: (Duration) -> Unit,
	modifier: Modifier = Modifier,
	onSeekEnd: () -> Unit = {},
	enabled: Boolean = true,
	interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
	val state = remember {
		SliderState(
			value = trackData().playRatio,
			valueRange = 0f..1f,
			onValueChangeFinished = onSeekEnd,
		).also { state ->
			// on value change will be called only when the value changed completed
			// via interactions not update
			state.onValueChange = { value ->
				val seekAmount = trackData().calculateSeekAmount(value)
				onSeek(seekAmount)
			}
		}
	}

	// update the track ratio when changed
	LaunchedEffect(state) {
		snapshotFlow { trackData().playRatio }
			.onEach { value -> state.value = value }
			.launchIn(this)
	}

	Slider(
		state = state,
		enabled = enabled,
		colors = SliderDefaults.colors(
			activeTrackColor = MaterialTheme.colorScheme.primary,
			thumbColor = MaterialTheme.colorScheme.primary,
			inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
		),
		modifier = modifier,
		interactionSource = interactionSource,
	)
}

@Preview
@Composable
private fun PlayerTrackSlider2Preview() = RecorderAppTheme {
	var trackState by remember { mutableStateOf(PlayerTrackData(0.seconds, 10.seconds)) }

	PlayerTrackSlider2(
		trackData = { trackState },
		onSeek = { trackState = trackState.copy(current = it) },
	)
}