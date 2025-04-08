package com.eva.feature_widget.recordings.composables

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
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.eva.feature_widget.R
import com.eva.feature_widget.utils.GlancePreviewRecordings
import com.eva.feature_widget.utils.RecorderAppWidgetTheme

@Composable
@GlanceComposable
internal fun RecordingsLoadError(
	message: String? = null,
	modifier: GlanceModifier = GlanceModifier,
) {

	val context = LocalContext.current

	Column(
		modifier = modifier,
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalAlignment = Alignment.CenterVertically
	) {
		Image(
			provider = ImageProvider(R.drawable.ic_widget_error),
			contentDescription = null,
			colorFilter = ColorFilter.tint(colorProvider = GlanceTheme.colors.secondary),
			modifier = GlanceModifier.size(28.dp)
		)
		Spacer(modifier = GlanceModifier.height(8.dp))
		Text(
			text = context.getString(R.string.recordings_load_failed),
			style = TextStyle(
				color = GlanceTheme.colors.primary,
				fontWeight = FontWeight.Medium,
				fontSize = 16.sp
			)
		)
		Spacer(modifier = GlanceModifier.height(4.dp))
		Text(
			text = message ?: context.getString(R.string.widget_error_text),
			style = TextStyle(
				color = GlanceTheme.colors.onSurfaceVariant,
				fontWeight = FontWeight.Normal,
				fontSize = 14.sp
			)
		)
	}
}

@GlancePreviewRecordings
@Composable
private fun RecordingsLoadErrorPreview() = RecorderAppWidgetTheme {
	RecordingsLoadError(message = "Failed to load", modifier = GlanceModifier.fillMaxSize())
}