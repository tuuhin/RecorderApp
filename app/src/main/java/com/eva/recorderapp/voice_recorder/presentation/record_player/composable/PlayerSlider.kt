package com.eva.recorderapp.voice_recorder.presentation.record_player.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.common.LocalTimeFormats
import com.eva.recorderapp.ui.theme.DownloadableFonts
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.player.PlayerTrackData
import kotlinx.datetime.format
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

	val currentReadable by remember(track.current) {
		derivedStateOf {
			track.currentAsLocalTime
				.format(LocalTimeFormats.LOCALTIME_FORMAT_MM_SS)
		}
	}

	val negativeTimeReadable by remember(track.current, track.total) {
		derivedStateOf {
			track.leftDurationAsLocalTime
				.format(LocalTimeFormats.LOCALTIME_FORMAT_MM_SS)
		}
	}

	Column(
		modifier = modifier.padding(horizontal = 12.dp),
	) {
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			Text(
				text = currentReadable,
				color = MaterialTheme.colorScheme.secondary,
				style = MaterialTheme.typography.titleSmall,
				fontFamily = DownloadableFonts.NOVA_MONO_FONT_FAMILY
			)
			Text(
				text = negativeTimeReadable,
				style = MaterialTheme.typography.titleSmall,
				color = MaterialTheme.colorScheme.tertiary,
				fontFamily = DownloadableFonts.NOVA_MONO_FONT_FAMILY
			)
		}

		Slider(
			value = sliderPercentage,
			onValueChange = { seekAmt ->
				val seek = track.calculateSeekAmount(seekAmt)
				onSeekToDuration(seek.milliseconds)
			},
			onValueChangeFinished = onSeekDurationComplete,
			colors = SliderDefaults.colors(
				activeTrackColor = MaterialTheme.colorScheme.primary,
				thumbColor = MaterialTheme.colorScheme.primary,
				inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
			),
		)
	}
}


@PreviewLightDark
@Composable
private fun PlayerSliderPreview() = RecorderAppTheme {
	Surface {
		PlayerSlider(
			track = PlayerTrackData(),
			onSeekToDuration = {},
		)
	}
}