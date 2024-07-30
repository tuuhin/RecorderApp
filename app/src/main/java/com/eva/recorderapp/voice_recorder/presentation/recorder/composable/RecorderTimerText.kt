package com.eva.recorderapp.voice_recorder.presentation.recorder.composable

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eva.recorderapp.common.LocalTimeFormats
import com.eva.recorderapp.ui.theme.DownloadableFonts
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format

@Composable
fun RecorderTimerText(
	time: LocalTime,
	modifier: Modifier = Modifier,
	style: TextStyle = MaterialTheme.typography.displayLarge,
	color: Color = MaterialTheme.colorScheme.onBackground
) {
// TODO: Change the color of each of the units
	val timeText by remember(time) {
		derivedStateOf {
			if (time.hour > 0)
				time.format(LocalTimeFormats.LOCALTIME_FORMAT_HH_MM_SS_SF2)
			time.format(LocalTimeFormats.LOCALTIME_FORMAT_MM_SS_SF2)
		}
	}

	Text(
		text = timeText,
		modifier = modifier.padding(horizontal = 4.dp),
		style = style,
		color = color,
		fontFamily = DownloadableFonts.CLOCK_FACE,
		letterSpacing = 2.sp
	)
}