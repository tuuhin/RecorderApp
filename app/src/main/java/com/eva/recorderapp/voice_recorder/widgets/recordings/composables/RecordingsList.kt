package com.eva.recorderapp.voice_recorder.widgets.recordings.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
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
	onItemClick: (RecordedVoiceModel) -> Unit,
	modifier: GlanceModifier = GlanceModifier,
) {
	val context = LocalContext.current

	if (recordings.isEmpty()) {
		Box(
			modifier = modifier,
			contentAlignment = Alignment.Center
		) {
			Text(
				text = context.getString(R.string.widget_recordings_no_recordings),
				style = TextStyle(
					color = GlanceTheme.colors.secondary,
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
			) { _, musicItem ->
				Box(
					modifier = GlanceModifier
						.padding(vertical = 4.dp)
						.fillMaxWidth()
				) {
					RecordingWidgetCard(
						musicItem = musicItem,
						onClick = { onItemClick(musicItem) },
						modifier = GlanceModifier.fillMaxWidth()
					)
				}
			}
		}
	}
}


@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 306, heightDp = 306)
@Composable
private fun RecordingsContentPreviewWithoutFavourite() =
	RecorderAppWidgetTheme {
		RecordingsList(
			recordings = PreviewFakes.FAKE_VOICE_RECORDING_MODELS.map { it.recoding },
			onItemClick = {},
			modifier = GlanceModifier.fillMaxSize()
		)
	}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 306, heightDp = 306)
@Composable
private fun RecordingsContentPreviewWithFavourite() =
	RecorderAppWidgetTheme {
		RecordingsList(
			recordings = PreviewFakes.FAKE_VOICE_RECORDINGS_SELECTED.map { it.recoding },
			onItemClick = {},
			modifier = GlanceModifier.fillMaxSize()
		)
	}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 306, heightDp = 306)
@Composable
private fun RecordingsContentPreviewEmpty() =
	RecorderAppWidgetTheme {
		RecordingsList(
			recordings = emptyList(),
			onItemClick = {},
			modifier = GlanceModifier.fillMaxSize()
		)
	}