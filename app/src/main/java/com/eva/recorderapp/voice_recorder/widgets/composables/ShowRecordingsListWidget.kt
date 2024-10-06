package com.eva.recorderapp.voice_recorder.widgets.composables

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
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.eva.recorderapp.R
import com.eva.recorderapp.common.LocalTimeFormats.NOTIFICATION_TIMER_TIME_FORMAT
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import kotlinx.datetime.format

@Composable
@GlanceComposable
fun ShowCurrentRecordingsWidget(
	recordings: List<RecordedVoiceModel>,
	onItemClick: (RecordedVoiceModel) -> Unit,
	modifier: GlanceModifier = GlanceModifier,
) {
	val context = LocalContext.current

	Scaffold(
		titleBar = {
			TitleBar(
				startIcon = ImageProvider(R.drawable.ic_recorder),
				title = context.getString(R.string.recording_top_bar_title),
				actions = {
					CircleIconButton(
						imageProvider = ImageProvider(R.drawable.ic_widget_refresh),
						contentDescription = context.getString(R.string.widget_refresh),
						onClick = {},
						backgroundColor = GlanceTheme.colors.widgetBackground,
						contentColor = GlanceTheme.colors.primary,
					)
				},
				iconColor = GlanceTheme.colors.primary,
				textColor = GlanceTheme.colors.onBackground,
			)
		},
		backgroundColor = GlanceTheme.colors.widgetBackground,
		horizontalPadding = 10.dp,
		modifier = modifier,
	) {
		LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
			itemsIndexed(
				items = recordings,
				itemId = { _, item -> item.id },
			) { _, musicItem ->
				Box(
					modifier = GlanceModifier.padding(vertical = 4.dp)
						.fillMaxWidth()
				) {
					Row(
						modifier = GlanceModifier
							.padding(vertical = 8.dp, horizontal = 12.dp)
							.fillMaxWidth()
							.cornerRadius(16.dp)
							.background(GlanceTheme.colors.primaryContainer)
							.clickable { onItemClick(musicItem) },
						verticalAlignment = Alignment.CenterVertically
					) {
						Image(
							provider = ImageProvider(R.drawable.ic_widget_mic),
							contentDescription = null,
							modifier = GlanceModifier.size(24.dp),
							colorFilter = ColorFilter.tint(colorProvider = GlanceTheme.colors.onPrimaryContainer)
						)
						Spacer(modifier = GlanceModifier.width(8.dp))
						Column(modifier = GlanceModifier.defaultWeight()) {
							Text(
								text = musicItem.title,
								style = TextStyle(
									color = GlanceTheme.colors.onPrimaryContainer,
									fontWeight = FontWeight.Medium,
									fontSize = 16.sp,
								),
								maxLines = 1,
							)
							Text(
								text = musicItem.durationAsLocaltime.format(
									NOTIFICATION_TIMER_TIME_FORMAT
								),
								style = TextStyle(
									color = GlanceTheme.colors.onPrimaryContainer,
									fontWeight = FontWeight.Normal,
									fontSize = 12.sp
								),
							)
						}
						if (musicItem.isFavorite) {
							Image(
								provider = ImageProvider(R.drawable.ic_star_outlined),
								contentDescription = context.getString(R.string.menu_option_favourite),
								modifier = GlanceModifier.size(20.dp),
								colorFilter = ColorFilter.tint(colorProvider = GlanceTheme.colors.onPrimaryContainer)
							)
						}
					}
				}
			}
		}
	}
}


@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 306, heightDp = 422)
@Composable
private fun ShowCurrentRecordingsWidgetPreviewWithoutFavourite() = RecorderWidgetTheme {
	ShowCurrentRecordingsWidget(
		recordings = List(10) {
			PreviewFakes.FAKE_VOICE_RECORDING_MODEL.copy(id = it.toLong())
		},
		onItemClick = {},
	)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 306, heightDp = 422)
@Composable
private fun ShowCurrentRecordingsWidgetPreviewWithFavourite() = RecorderWidgetTheme {
	ShowCurrentRecordingsWidget(
		recordings = List(10) {
			PreviewFakes.FAKE_VOICE_RECORDING_MODEL.copy(id = it.toLong(), isFavorite = true)
		},
		onItemClick = {},
	)
}