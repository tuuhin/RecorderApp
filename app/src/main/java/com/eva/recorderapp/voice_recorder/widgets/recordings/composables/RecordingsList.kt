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
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import com.eva.recorderapp.voice_recorder.widgets.utils.RecorderAppWidgetTheme

@GlanceComposable
@Composable
fun RecordingsList(
	recordings: List<RecordedVoiceModel>,
	modifier: GlanceModifier = GlanceModifier,
) {
	val context = LocalContext.current

	if (recordings.isEmpty()) {
		Column(
			modifier = modifier,
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalAlignment = Alignment.CenterVertically
		) {
			Image(
				provider = ImageProvider(R.drawable.ic_recorder),
				contentDescription = context.getString(R.string.no_recordings),
				colorFilter = ColorFilter.tint(GlanceTheme.colors.secondary),
				modifier = GlanceModifier.size(48.dp)
			)
			Spacer(modifier = GlanceModifier.height(4.dp))
			Text(
				text = context.getString(R.string.widget_recordings_no_recordings),
				style = TextStyle(
					color = GlanceTheme.colors.tertiary,
					fontWeight = FontWeight.Medium,
					fontSize = 16.sp
				),
			)
		}
	} else {
		LazyColumn(modifier = modifier) {
			itemsIndexed(
				items = recordings,
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
			recordings = PreviewFakes.FAKE_VOICE_RECORDING_MODELS.map { it.recoding },
			modifier = GlanceModifier.fillMaxSize()
		)
	}

@GlancePreviewRecordings
@Composable
private fun RecordingsContentPreviewWithFavourite() =
	RecorderAppWidgetTheme {
		RecordingsList(
			recordings = PreviewFakes.FAKE_VOICE_RECORDINGS_SELECTED.map { it.recoding },
			modifier = GlanceModifier.fillMaxSize()
		)
	}

@GlancePreviewRecordings
@Composable
private fun RecordingsContentPreviewEmpty() =
	RecorderAppWidgetTheme {
		RecordingsList(
			recordings = emptyList(),
			modifier = GlanceModifier.fillMaxSize()
		)
	}