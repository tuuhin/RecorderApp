package com.eva.player_shared.composables

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eva.player.domain.model.PlayerTrackData
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.math.round
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
@Composable
fun PlayerSlider(
	trackData: PlayerTrackData,
	onSeekComplete: (Duration) -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true
) {
	val controller = remember { PlayerSliderController() }
	var sliderPosition by remember { mutableFloatStateOf(trackData.playRatio) }

	val isUserControlled by controller.isSeekByUser.collectAsStateWithLifecycle(false)
	val seekAmountByUser by controller.seekAmountByUser.collectAsStateWithLifecycle()

	LaunchedEffect(trackData.playRatio, isUserControlled, seekAmountByUser) {
		val newTrackData = if (isUserControlled) {
			trackData.copy(current = seekAmountByUser)
		} else trackData

		snapshotFlow { newTrackData }
			.map { it.playRatio.roundToNDecimals() }
			.distinctUntilChanged()
			.collectLatest {
				sliderPosition = it
			}
	}

	Slider(
		value = sliderPosition,
		onValueChange = { newPosition ->
			val playerSeekAmount = trackData.calculateSeekAmount(newPosition)
			controller.onSliderSlide(playerSeekAmount)
		},
		onValueChangeFinished = {
			val finalAmount = controller.onSliderSlideComplete()
			onSeekComplete(finalAmount)
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

@OptIn(FlowPreview::class)
private class PlayerSliderController {

	val seekAmountByUser = MutableStateFlow(0.seconds)
	private val _isSeekByUser = MutableStateFlow(false)

	val isSeekByUser = _isSeekByUser.debounce(120.milliseconds)
		.distinctUntilChanged()

	fun onSliderSlide(amount: Duration) {
		_isSeekByUser.update { true }
		seekAmountByUser.update { amount }
	}

	fun onSliderSlideComplete(): Duration {
		_isSeekByUser.update { false }
		return seekAmountByUser.value
	}
}