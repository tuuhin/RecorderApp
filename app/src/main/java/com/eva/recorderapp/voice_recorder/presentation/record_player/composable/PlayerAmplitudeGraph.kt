package com.eva.recorderapp.voice_recorder.presentation.record_player.composable

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun PlayerAmplitudeGraph(
	modifier: Modifier = Modifier,
	color: Color = MaterialTheme.colorScheme.primaryContainer,
	shape: Shape = MaterialTheme.shapes.medium
) {
	Surface(
		color = color,
		shape = shape,
		contentColor = contentColorFor(backgroundColor = color),
		modifier = modifier.aspectRatio(1.5f),
	) {
		Spacer(modifier = Modifier.defaultMinSize(minHeight = 120.dp))
	}
}