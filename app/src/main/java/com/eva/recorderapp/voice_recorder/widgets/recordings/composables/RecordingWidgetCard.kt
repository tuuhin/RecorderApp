package com.eva.recorderapp.voice_recorder.widgets.recordings.composables

import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.eva.recorderapp.R
import com.eva.recorderapp.common.LocalTimeFormats.NOTIFICATION_TIMER_TIME_FORMAT
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import kotlinx.datetime.format

@GlanceComposable
@Composable
fun RecordingWidgetCard(
	model: RecordedVoiceModel,
	modifier: GlanceModifier = GlanceModifier,
) {

	val context = LocalContext.current

	val fileSize = remember(model.sizeInBytes) {
		Formatter.formatShortFileSize(context, model.sizeInBytes)
	}

	Row(
		modifier = modifier
			.padding(all = 8.dp)
			.cornerRadius(16.dp)
			.background(GlanceTheme.colors.primaryContainer),
		verticalAlignment = Alignment.CenterVertically
	) {
		Box(
			modifier = GlanceModifier
				.size(28.dp)
				.cornerRadius(8.dp)
				.background(GlanceTheme.colors.primary)
				.padding(4.dp),
			contentAlignment = Alignment.Center
		) {
			Image(
				provider = ImageProvider(R.drawable.ic_widget_mic),
				contentDescription = null,
				modifier = GlanceModifier.size(24.dp),
				colorFilter = ColorFilter.tint(colorProvider = GlanceTheme.colors.onPrimary)
			)
		}
		Spacer(modifier = GlanceModifier.width(8.dp))
		Column(modifier = GlanceModifier.defaultWeight()) {
			Text(
				text = model.title,
				style = TextStyle(
					color = GlanceTheme.colors.onPrimaryContainer,
					fontWeight = FontWeight.Medium,
					fontSize = 14.sp,
				),
				maxLines = 1,
			)
			Row(modifier = GlanceModifier.wrapContentHeight()) {
				Text(
					text = model.durationAsLocaltime.format(NOTIFICATION_TIMER_TIME_FORMAT),
					style = TextStyle(
						color = GlanceTheme.colors.onPrimaryContainer,
						fontWeight = FontWeight.Normal,
						fontSize = 10.sp
					),
				)
				Spacer(modifier = GlanceModifier.width(12.dp))
				Text(
					text = fileSize,
					style = TextStyle(
						color = GlanceTheme.colors.onPrimaryContainer,
						fontWeight = FontWeight.Normal,
						fontSize = 10.sp
					),
				)
			}
		}
		if (model.isFavorite) {
			Image(
				provider = ImageProvider(R.drawable.ic_star_filled),
				contentDescription = context.getString(R.string.menu_option_favourite),
				modifier = GlanceModifier.size(16.dp),
				colorFilter = ColorFilter.tint(colorProvider = GlanceTheme.colors.primary)
			)
		}
	}
}