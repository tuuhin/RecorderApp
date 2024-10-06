package com.eva.recorderapp.voice_recorder.widgets.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.Text

@Composable
@GlanceComposable
fun RecorderWidget(modifier: GlanceModifier = GlanceModifier) {
	Scaffold(
		backgroundColor = GlanceTheme.colors.primaryContainer,
		modifier = modifier,
		horizontalPadding = 12.dp
	) {
		Row(
			modifier = GlanceModifier
				.fillMaxSize()
				.padding(vertical = 12.dp),
		) {
			Text("0.0")
		}
	}
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(heightDp = 130, widthDp = 624)
@Composable
private fun RecorderWidgetPreview() = RecorderWidgetTheme {
	RecorderWidget()
}