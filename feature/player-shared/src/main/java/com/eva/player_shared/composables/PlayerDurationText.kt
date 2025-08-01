package com.eva.player_shared.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.eva.player.domain.model.PlayerTrackData
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.utils.LocalTimeFormats
import kotlinx.coroutines.FlowPreview
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format

@OptIn(FlowPreview::class)
@Composable
private fun PlayerDurationText(
	playedDuration: Long,
	totalDuration: Long,
	modifier: Modifier = Modifier,
	fontFamily: FontFamily = FontFamily.Monospace,
) {
	val totalDurationText by remember(totalDuration) {
		derivedStateOf {
			val time = LocalTime.fromMillisecondOfDay(totalDuration.toInt())
			with(time) {
				format(LocalTimeFormats.LOCALTIME_HH_MM_SS_FORMAT)
			}
		}
	}

	val playedDurationText by remember(playedDuration) {
		derivedStateOf {
			val time = LocalTime.fromMillisecondOfDay(playedDuration.toInt())
			with(time) {
				if (hour > 0) format(LocalTimeFormats.LOCALTIME_FORMAT_HH_MM_SS_SF2)
				format(LocalTimeFormats.LOCALTIME_FORMAT_MM_SS_SF2)
			}
		}
	}



	Column(
		modifier = modifier.padding(horizontal = 10.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Text(
			text = playedDurationText,
			modifier = modifier,
			style = MaterialTheme.typography.displayMedium,
			color = MaterialTheme.colorScheme.primary,
			fontFamily = fontFamily,
		)
		Text(
			text = totalDurationText,
			style = MaterialTheme.typography.headlineSmall,
			color = MaterialTheme.colorScheme.secondary,
			fontFamily = fontFamily
		)
	}
}

@Composable
fun PlayerDurationText(
	track: PlayerTrackData,
	fileModel: AudioFileModel,
	modifier: Modifier = Modifier,
	fontFamily: FontFamily = FontFamily.Monospace,
) {
	PlayerDurationText(
		playedDuration = track.current.inWholeMilliseconds,
		totalDuration = fileModel.duration.inWholeMilliseconds,
		fontFamily = fontFamily,
		modifier = modifier
	)
}

@Composable
fun PlayerDurationText(
	track: PlayerTrackData,
	modifier: Modifier = Modifier,
	fontFamily: FontFamily = FontFamily.Monospace,
) {
	PlayerDurationText(
		playedDuration = track.current.inWholeMilliseconds,
		totalDuration = track.total.inWholeMilliseconds,
		modifier = modifier,
		fontFamily = fontFamily,
	)
}