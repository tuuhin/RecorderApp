package com.eva.recorderapp.voice_recorder.widgets.recordings.composables

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
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.eva.recorderapp.R
import com.eva.recorderapp.common.LocalTimeFormats.NOTIFICATION_TIMER_TIME_FORMAT
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.widgets.utils.maybeCornerRadius
import kotlinx.datetime.format

@GlanceComposable
@Composable
fun RecordingWidgetCard(
	musicItem: RecordedVoiceModel,
	onClick: () -> Unit,
	modifier: GlanceModifier = GlanceModifier,
) {

	val context = LocalContext.current

	Row(
		modifier = modifier
			.padding(all = 4.dp)
			.maybeCornerRadius(16.dp, resId = R.drawable.rounded_shape_primary_cont_color)
			.background(GlanceTheme.colors.primaryContainer)
			.clickable(block = onClick),
		verticalAlignment = Alignment.CenterVertically
	) {
		Box(
			modifier = GlanceModifier
				.size(28.dp)
				.maybeCornerRadius(8.dp, resId = R.drawable.rounded_shape_primary_color)
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
		Spacer(modifier = GlanceModifier.width(12.dp))
		Column(modifier = GlanceModifier.defaultWeight()) {
			Text(
				text = musicItem.title,
				style = TextStyle(
					color = GlanceTheme.colors.onPrimaryContainer,
					fontWeight = FontWeight.Medium,
					fontSize = 14.sp,
				),
				maxLines = 1,
			)
			Text(
				text = musicItem.durationAsLocaltime.format(NOTIFICATION_TIMER_TIME_FORMAT),
				style = TextStyle(
					color = GlanceTheme.colors.onPrimaryContainer,
					fontWeight = FontWeight.Normal,
					fontSize = 10.sp
				),
			)
		}
		if (musicItem.isFavorite) {
			Image(
				provider = ImageProvider(R.drawable.ic_star_outlined),
				contentDescription = context.getString(R.string.menu_option_favourite),
				modifier = GlanceModifier.size(24.dp),
				colorFilter = ColorFilter.tint(colorProvider = GlanceTheme.colors.onPrimaryContainer)
			)
		}
	}
}