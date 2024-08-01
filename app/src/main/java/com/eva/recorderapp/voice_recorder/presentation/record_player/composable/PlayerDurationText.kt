package com.eva.recorderapp.voice_recorder.presentation.record_player.composable

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eva.recorderapp.common.LocalTimeFormats
import com.eva.recorderapp.ui.theme.DownloadableFonts
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format

@Composable
fun PlayerDurationText(
	playedDuration: LocalTime,
	totalDuration: LocalTime,
	modifier: Modifier = Modifier
) {
	val totalDurationText by remember(totalDuration) {
		derivedStateOf {
			totalDuration.format(LocalTimeFormats.LOCALTIME_HH_MM_SS_FORMAT)
		}
	}

	val playedDurationText by remember(playedDuration) {
		derivedStateOf {
			if (playedDuration.hour > 0)
				playedDuration.format(LocalTimeFormats.LOCALTIME_FORMAT_HH_MM_SS_SF2)
			playedDuration.format(LocalTimeFormats.LOCALTIME_FORMAT_MM_SS_SF2)
		}
	}

	Column(
		modifier = modifier.padding(horizontal = 10.dp),
		verticalArrangement = Arrangement.spacedBy(6.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Text(
			text = playedDurationText,
			modifier = modifier,
			style = MaterialTheme.typography.displayMedium,
			color = MaterialTheme.colorScheme.secondary,
			fontFamily = DownloadableFonts.CLOCK_FACE,
			letterSpacing = 2.sp
		)
		Text(
			text = totalDurationText,
			style = MaterialTheme.typography.displaySmall,
			color = MaterialTheme.colorScheme.secondary,
		)
	}
}