package com.eva.recorderapp.voice_recorder.presentation.recorder.composable

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.common.LocalTimeFormats
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format

@Composable
fun RecorderTimerText(
	time: LocalTime,
	modifier: Modifier = Modifier
) {

	val timeText by remember(time) {
		derivedStateOf {
			time.format(LocalTimeFormats.PRESENTATON_TIMER_TIME_FORMAT)
		}
	}

	Text(
		text = timeText,
		modifier = modifier.padding(horizontal = 20.dp, vertical = 10.dp),
		style = MaterialTheme.typography.displayLarge,
	)
}