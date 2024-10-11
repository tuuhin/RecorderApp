package com.eva.recorderapp.voice_recorder.widgets.recordings.composables

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import com.eva.recorderapp.R
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.ResourcedVoiceRecordingModels
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import com.eva.recorderapp.voice_recorder.widgets.utils.RecorderAppWidgetTheme

@Composable
@GlanceComposable
fun RecordingsWidgetContent(
	resource: ResourcedVoiceRecordingModels,
	onRefresh: () -> Unit = {},
	modifier: GlanceModifier = GlanceModifier,
) {
	val context = LocalContext.current
	val background = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
		GlanceTheme.colors.widgetBackground
	else GlanceTheme.colors.background

	Scaffold(
		titleBar = {
			TitleBar(
				startIcon = ImageProvider(R.drawable.ic_recorder),
				title = context.getString(R.string.recording_top_bar_title),
				actions = {
					CircleIconButton(
						imageProvider = ImageProvider(R.drawable.ic_widget_refresh),
						contentDescription = context.getString(R.string.widget_refresh),
						onClick = onRefresh,
						backgroundColor =background,
						contentColor = GlanceTheme.colors.primary,
					)
				},
				iconColor = GlanceTheme.colors.primary,
				textColor = GlanceTheme.colors.onSurface,
			)
		},
		backgroundColor = background,
		horizontalPadding = 12.dp,
		modifier = modifier,
	) {
		when (resource) {
			Resource.Loading -> {
				Box(
					modifier = GlanceModifier.fillMaxSize(),
					contentAlignment = Alignment.Center
				) {
					CircularProgressIndicator(color = GlanceTheme.colors.secondary)
				}
			}

			is Resource.Error -> RecordingsLoadError(
				message = resource.message,
				modifier = GlanceModifier.fillMaxSize()
			)

			is Resource.Success -> RecordingsList(
				recordings = resource.data,
				modifier = GlanceModifier.fillMaxSize()
			)
		}
	}
}


@GlancePreviewRecordings
@Composable
private fun RecordingsContentPreviewResourceSuccess() = RecorderAppWidgetTheme {
	RecordingsWidgetContent(
		resource = Resource.Success(data = PreviewFakes.FAKE_VOICE_RECORDING_MODELS.map { it.recoding }),
		onRefresh = {},
	)
}