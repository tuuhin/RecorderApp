package com.eva.player_shared.composables

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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eva.player.domain.model.PlayerTrackData
import com.eva.player_shared.state.PlayerSliderController
import com.eva.ui.theme.RecorderAppTheme
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerTrackSlider2(
	trackData: () -> PlayerTrackData,
	onSeekComplete: (Duration) -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true
) {
	// slider controller
	val controller = remember { PlayerSliderController() }
	val isUserControlled by controller.isSeekByUser.collectAsStateWithLifecycle(false)
	val seekAmountByUser by controller.seekAmountByUser.collectAsStateWithLifecycle()

	val currentOnSeekComplete by rememberUpdatedState(onSeekComplete)

	val state = remember { SliderState(value = trackData().playRatio) }

	LaunchedEffect(state) {

		// Basic state update updated by the player
		snapshotFlow { trackData().playRatio }
			.filter { !state.isDragging }
			.onEach { state.value = it }
			.launchIn(this)

		// If the slider is being drag , updated by the user
		snapshotFlow { state.value }
			.filter { state.isDragging }
			.onEach { seek ->
				val playerSeekAmount = trackData().calculateSeekAmount(seek)
				controller.onSliderSlide(playerSeekAmount)
			}.launchIn(this)

		// now if it's not being dragged  but user controlled then send seek completed
		snapshotFlow { !state.isDragging && isUserControlled }
			.filter { it }
			.onEach {
				controller.sliderCleanUp()
				currentOnSeekComplete(seekAmountByUser)
			}
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
		modifier = modifier
	)
}

@Preview
@Composable
private fun PlayerTrackSlider2Preview() = RecorderAppTheme {
	var trackState by remember { mutableStateOf(PlayerTrackData(0.seconds, 10.seconds)) }

	PlayerTrackSlider2(
		trackData = { trackState },
		onSeekComplete = { trackState = trackState.copy(current = it) },
	)
}