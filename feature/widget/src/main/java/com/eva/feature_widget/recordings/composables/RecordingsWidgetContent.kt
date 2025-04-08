package com.eva.feature_widget.recordings.composables

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
import com.eva.feature_widget.R
import com.eva.feature_widget.recordings.RecordedModelsList
import com.eva.feature_widget.utils.GlancePreviewRecordings
import com.eva.feature_widget.utils.RecorderAppWidgetTheme
import com.eva.feature_widget.utils.WidgetPreviewFakes
import com.eva.recordings.domain.provider.ResourcedVoiceRecordingModels
import com.eva.utils.Resource

@Composable
@GlanceComposable
internal fun RecordingsWidgetContent(
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
				startIcon = ImageProvider(R.drawable.ic_widget_recorder),
				title = context.getString(R.string.widget_recordings_title),
				actions = {
					CircleIconButton(
						imageProvider = ImageProvider(R.drawable.ic_widget_refresh),
						contentDescription = context.getString(R.string.widget_refresh),
						onClick = onRefresh,
						backgroundColor = background,
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
				recordings = RecordedModelsList(resource.data),
				modifier = GlanceModifier.fillMaxSize()
			)
		}
	}
}


@GlancePreviewRecordings
@Composable
private fun RecordingsContentPreviewResourceSuccess() = RecorderAppWidgetTheme {
	RecordingsWidgetContent(
		resource = Resource.Success(data = WidgetPreviewFakes.FAKE_VOICE_RECORDING_MODELS.recordings),
		onRefresh = {},
	)
}