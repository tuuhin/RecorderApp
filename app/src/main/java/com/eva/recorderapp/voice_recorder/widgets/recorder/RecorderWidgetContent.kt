package com.eva.recorderapp.voice_recorder.widgets.recorder

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentSize
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.eva.recorderapp.R
import com.eva.recorderapp.common.LocalTimeFormats
import com.eva.recorderapp.voice_recorder.domain.recorder.emums.RecorderState
import com.eva.recorderapp.voice_recorder.widgets.data.RecorderWidgetModel
import com.eva.recorderapp.voice_recorder.widgets.utils.RecorderAppWidgetTheme
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format

@Composable
@GlanceComposable
fun RecorderWidgetContent(
	model: RecorderWidgetModel,
	modifier: GlanceModifier = GlanceModifier,
) {
	val context = LocalContext.current

	val background = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
		GlanceTheme.colors.widgetBackground
	else GlanceTheme.colors.background

	Scaffold(
		backgroundColor = background,
		modifier = modifier,
		horizontalPadding = 12.dp
	) {
		Row(
			modifier = GlanceModifier
				.fillMaxSize()
				.padding(vertical = 12.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalAlignment = Alignment.CenterVertically,
		) {
			Box(
				modifier = GlanceModifier
					.size(48.dp)
					.cornerRadius(8.dp)
					.background(GlanceTheme.colors.primaryContainer)
					.padding(4.dp),
				contentAlignment = Alignment.Center
			) {
				Image(
					provider = ImageProvider(R.drawable.ic_mic_variant),
					contentDescription = context.getString(R.string.widget_recorder_widget),
					modifier = GlanceModifier.size(32.dp),
					colorFilter = ColorFilter.tint(colorProvider = GlanceTheme.colors.onPrimaryContainer)
				)
			}
			Spacer(modifier = GlanceModifier.width(20.dp))
			Column(
				modifier = GlanceModifier.wrapContentSize(),
				horizontalAlignment = Alignment.Start
			) {
				Text(
					text = model.time.format(LocalTimeFormats.LOCALTIME_FORMAT_MM_SS),
					style = TextStyle(
						color = GlanceTheme.colors.onBackground,
						fontWeight = FontWeight.Medium,
						fontSize = 18.sp
					)
				)
				Spacer(modifier = GlanceModifier.height(4.dp))

				val currentState = when (model.state) {
					RecorderState.RECORDING -> context.getString(R.string.recorder_state_recording)
					RecorderState.PAUSED -> context.getString(R.string.recorder_state_paused)
					else -> null
				}

				currentState?.let {
					Text(
						text = it,
						style = TextStyle(
							color = GlanceTheme.colors.onSurfaceVariant,
							fontWeight = FontWeight.Normal,
							fontSize = 14.sp
						),
						maxLines = 1,
					)
				}
			}
		}
	}
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(heightDp = 130, widthDp = 245)
@Composable
private fun RecorderWidgetPreview() = RecorderAppWidgetTheme {
	RecorderWidgetContent(
		model = RecorderWidgetModel(
			state = RecorderState.RECORDING,
			time = LocalTime.fromSecondOfDay(5)
		)
	)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(heightDp = 130, widthDp = 245)
@Composable
private fun RecorderWidgetRecordingPreview() = RecorderAppWidgetTheme {
	RecorderWidgetContent(
		model = RecorderWidgetModel(
			state = RecorderState.PAUSED,
			time = LocalTime.fromSecondOfDay(5)
		)
	)
}