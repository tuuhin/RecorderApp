package com.eva.player_shared.composables

import androidx.compose.foundation.interaction.MutableInteractionSource
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PlayerTrackSlider2(
    trackData: () -> PlayerTrackData,
    onSeek: (Duration) -> Unit,
    modifier: Modifier = Modifier,
    onSeekEnd: () -> Unit = {},
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val state = remember {
        SliderState(value = trackData().playRatio, trackRange = 0f..1f)
    }

    // update the track ratio when changed
    LaunchedEffect(state) {
        snapshotFlow { trackData().playRatio }
            .collectLatest { value -> state.value = value }
    }

    Slider(
        state = state,
        onValueChange = { value ->
            val seekAmount = trackData().calculateSeekAmount(value)
            onSeek(seekAmount)
        },
        onValueChangeFinished = onSeekEnd,
        enabled = enabled,
        colors = SliderDefaults.colors(
            activeTrackColor = MaterialTheme.colorScheme.primary,
            thumbColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant,
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
