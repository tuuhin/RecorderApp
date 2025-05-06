package com.eva.feature_editor.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.eva.editor.domain.model.AudioClipConfig
import com.eva.feature_editor.event.EditorScreenEvent
import com.eva.ui.composables.DurationText
import com.eva.ui.theme.DownloadableFonts
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
private fun AudioClipChip(
	duration: Duration,
	modifier: Modifier = Modifier,
	onMinus: (Duration) -> Unit = {},
	onPlus: (Duration) -> Unit = {},
	containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
	contentColor: Color = MaterialTheme.colorScheme.onSurface,
	cornerShape: CornerBasedShape = MaterialTheme.shapes.small,
	numberFontFamily: FontFamily? = DownloadableFonts.CLOCK_FACE,
) {
	Row(
		modifier = modifier.wrapContentWidth(),
		verticalAlignment = Alignment.CenterVertically
	) {
		Surface(
			onClick = { onMinus(duration - 1.seconds) },
			shape = cornerShape.copy(topStart = CornerSize(24.dp), bottomStart = CornerSize(24.dp)),
			color = containerColor,
			contentColor = contentColor,
			modifier = Modifier.sizeIn(minWidth = 32.dp, minHeight = 32.dp),
		) {
			Box(contentAlignment = Alignment.Center) {
				Icon(
					imageVector = Icons.Default.Remove,
					contentDescription = "Subtract  Action",
					modifier = Modifier.size(24.dp)
				)
			}
		}
		Surface(
			color = containerColor,
			contentColor = contentColor,
			shape = cornerShape,
			modifier = Modifier.sizeIn(minHeight = 32.dp),
		) {
			Box(contentAlignment = Alignment.Center) {
				DurationText(
					duration = duration,
					fontFamily = numberFontFamily,
					modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
				)
			}
		}

		Surface(
			onClick = { onPlus(duration + 1.seconds) },
			shape = cornerShape.copy(topEnd = CornerSize(24.dp), bottomEnd = CornerSize(24.dp)),
			color = containerColor,
			contentColor = contentColor,
			modifier = Modifier.sizeIn(minWidth = 32.dp, minHeight = 32.dp),
		) {
			Box(contentAlignment = Alignment.Center) {
				Icon(
					imageVector = Icons.Default.Add,
					contentDescription = "Add Action",
					modifier = Modifier.size(24.dp)
				)
			}
		}
	}
}

@Composable
fun AudioClipChipRow(
	trackDuration: Duration,
	onEvent: (EditorScreenEvent) -> Unit,
	modifier: Modifier = Modifier,
	clipConfig: AudioClipConfig? = null,
) {
	val localClipConfig by remember(trackDuration, clipConfig) {
		val supposeToBe = AudioClipConfig(0.milliseconds, trackDuration)
		mutableStateOf(clipConfig ?: supposeToBe)
	}

	Row(
		modifier = modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.SpaceBetween
	) {
		// start clipper
		AudioClipChip(
			duration = localClipConfig.start,
			onPlus = { duration ->
				if (duration <= trackDuration && duration <= localClipConfig.end)
					onEvent(EditorScreenEvent.OnClipConfigChange(localClipConfig.copy(start = duration)))
			},
			onMinus = { duration ->
				val newStart = if (duration >= 0.seconds) duration else Duration.ZERO
				onEvent(EditorScreenEvent.OnClipConfigChange(localClipConfig.copy(start = newStart)))
			},
		)
		// end clipper
		AudioClipChip(
			duration = localClipConfig.end,
			onPlus = { duration ->
				val newEnd = if (duration <= trackDuration) duration else trackDuration
				onEvent(EditorScreenEvent.OnClipConfigChange(localClipConfig.copy(end = newEnd)))
			},
			onMinus = { duration ->
				if (duration >= 0.seconds && duration >= localClipConfig.start)
					onEvent(EditorScreenEvent.OnClipConfigChange(localClipConfig.copy(end = duration)))
			},
		)
	}
}
