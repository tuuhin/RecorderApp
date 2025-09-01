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
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.eva.feature_widget.R
import com.eva.feature_widget.recordings.RecordedModelsList
import com.eva.feature_widget.utils.GlancePreviewRecordings
import com.eva.feature_widget.utils.RecorderAppWidgetTheme
import com.eva.feature_widget.utils.WidgetPreviewFakes

@GlanceComposable
@Composable
internal fun RecordingsList(
	recordings: RecordedModelsList,
	modifier: GlanceModifier = GlanceModifier,
) {
	val context = LocalContext.current

	if (recordings.recordings.isEmpty()) {
		Column(
			modifier = modifier,
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalAlignment = Alignment.CenterVertically
		) {
			Image(
				provider = ImageProvider(R.drawable.ic_widget_recorder),
				contentDescription = context.getString(R.string.widget_no_recordings),
				colorFilter = ColorFilter.tint(GlanceTheme.colors.secondary),
				modifier = GlanceModifier.size(56.dp)
			)
			Spacer(modifier = GlanceModifier.height(8.dp))
			Text(
				text = context.getString(R.string.widget_recordings_no_recordings),
				style = TextStyle(
					color = GlanceTheme.colors.primary,
					fontWeight = FontWeight.Medium,
					fontSize = 20.sp
				),
			)
		}
	} else {
		LazyColumn(modifier = modifier) {
			itemsIndexed(
				items = recordings.recordings,
				itemId = { _, item -> item.id },
			) { _, item ->
				Box(
					modifier = GlanceModifier
						.padding(vertical = 4.dp)
						.fillMaxWidth()
				) {
					RecordingWidgetCard(
						model = item,
						modifier = GlanceModifier.fillMaxWidth()
					)
				}
			}
		}
	}
}


@GlancePreviewRecordings
@Composable
private fun RecordingsContentPreviewWithoutFavourite() =
	RecorderAppWidgetTheme {
		RecordingsList(
			recordings = WidgetPreviewFakes.FAKE_VOICE_RECORDING_MODELS,
			modifier = GlanceModifier.fillMaxSize()
		)
	}

@GlancePreviewRecordings
@Composable
private fun RecordingsContentPreviewWithFavourite() =
	RecorderAppWidgetTheme {
		RecordingsList(
			recordings = WidgetPreviewFakes.FAKE_VOICE_RECORDINGS_MODELS_WITH_FAVOURITES,
			modifier = GlanceModifier.fillMaxSize()
		)
	}

@GlancePreviewRecordings
@Composable
private fun RecordingsContentPreviewEmpty() =
	RecorderAppWidgetTheme {
		RecordingsList(
			recordings = RecordedModelsList(),
			modifier = GlanceModifier.fillMaxSize()
		)
	}