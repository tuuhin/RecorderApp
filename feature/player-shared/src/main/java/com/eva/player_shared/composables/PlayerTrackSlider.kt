package com.eva.player_shared.composables

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eva.player.domain.model.PlayerTrackData
import com.eva.player_shared.state.PlayerSliderController
import kotlinx.coroutines.launch
import kotlin.math.round
import kotlin.time.Duration

@Deprecated(
	message = "This version of the player track slider is im compatible with track data info,the player state is being hoisted outside the composable scope use PlayerTrackSlider2",
	replaceWith = ReplaceWith(
		"PlayerTrackSlider2",
		"com.eva.player_shared.composables.PlayerTrackSlider2"
	),
	level = DeprecationLevel.ERROR,
)
@Composable
fun PlayerTrackSlider(
	trackData: PlayerTrackData,
	onSeek: (Duration) -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true
) {
	val controller = remember { PlayerSliderController() }
	val scope = rememberCoroutineScope()

	var sliderPosition by remember { mutableFloatStateOf(trackData.playRatio) }

	val isUserControlled by controller.isSeekByUser.collectAsStateWithLifecycle(false)
	val seekAmountByUser by controller.seekAmountByUser.collectAsStateWithLifecycle()

	LaunchedEffect(trackData, isUserControlled, seekAmountByUser) {
		val newTrackData = if (isUserControlled) {
			trackData.copy(current = seekAmountByUser)
		} else trackData

		val playRatio = newTrackData.playRatio.roundToNDecimals()
		sliderPosition = playRatio
	}

	Slider(
		value = sliderPosition,
		onValueChange = { newPosition ->
			scope.launch {
				val playerSeekAmount = trackData.calculateSeekAmount(newPosition)
				controller.onSliderSlide(playerSeekAmount)
			}
		},
		onValueChangeFinished = {
			scope.launch {
				controller.sliderCleanUp {
					onSeek(seekAmountByUser)
				}
			}
		},
		colors = SliderDefaults.colors(
			activeTrackColor = MaterialTheme.colorScheme.primary,
			thumbColor = MaterialTheme.colorScheme.primary,
			inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
		),
		enabled = enabled,
		modifier = modifier
	)
}

private fun Float.roundToNDecimals(decimals: Int = 2): Float {
	var multiplier = 1.0f
	repeat(decimals) { multiplier *= 10 }
	return round(this * multiplier) / multiplier
}