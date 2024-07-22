package com.eva.recorderapp.voice_recorder.presentation.recordings.composable

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.common.LocalTimeFormats.NOTIFICATION_TIMER_TIME_FORMAT
import com.eva.recorderapp.common.LocalTimeFormats.RECORDING_RECORD_TIME_FORMAT
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import kotlinx.datetime.format

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecordingCard(
	music: RecordedVoiceModel,
	onItemClick: () -> Unit,
	onItemSelect: () -> Unit,
	modifier: Modifier = Modifier,
	isSelectable: Boolean = false,
	isSelected: Boolean = false,
	shape: Shape = MaterialTheme.shapes.medium,
) {

	val context = LocalContext.current

	val clickModifier = if (isSelectable)
		Modifier.clickable(onClick = onItemSelect, onClickLabel = "Item Selcted")
	else Modifier.combinedClickable(
		onClick = onItemClick,
		onLongClick = onItemSelect,
		onClickLabel = "Item Clicked",
		onLongClickLabel = "Item Selected"
	)

	val recordingInfo = remember(music) {
		if (music.isTrashed && music.expiresAt != null) {
			val time = music.expiresAt.format(RECORDING_RECORD_TIME_FORMAT)
			context.getString(R.string.recording_info_expires_at, time)
		} else music.recordedAt.format(RECORDING_RECORD_TIME_FORMAT)
	}


	ElevatedCard(
		shape = shape,
		elevation = CardDefaults.elevatedCardElevation(pressedElevation = 4.dp),
		modifier = modifier
			.clip(shape)
			.then(clickModifier),
	) {
		Row(
			horizontalArrangement = Arrangement.spacedBy(16.dp),
			verticalAlignment = Alignment.CenterVertically,
			modifier = Modifier
				.fillMaxWidth()
				.padding(all = dimensionResource(id = R.dimen.card_padding)),
		) {
			Crossfade(targetState = isSelectable) { showSelectOption ->
				if (showSelectOption) {
					RadioButton(
						selected = isSelected,
						onClick = onItemSelect,
						colors = RadioButtonDefaults
							.colors(selectedColor = MaterialTheme.colorScheme.secondary)

					)
				} else {
					Image(
						painter = painterResource(id = R.drawable.ic_play_variant),
						contentDescription = null,
						colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.primary),
						modifier = Modifier
							.size(48.dp)
							.padding(8.dp)
					)
				}
			}
			Column(
				modifier = Modifier.weight(1f),
				verticalArrangement = Arrangement.spacedBy(4.dp)
			) {
				Text(
					text = music.displayName,
					style = MaterialTheme.typography.titleSmall,
					color = MaterialTheme.colorScheme.secondary
				)
				Row(
					horizontalArrangement = Arrangement.SpaceBetween,
					verticalAlignment = Alignment.CenterVertically,
					modifier = Modifier.fillMaxWidth()
				) {
					Text(
						text = music.durationAsLocaltime.format(NOTIFICATION_TIMER_TIME_FORMAT),
						style = MaterialTheme.typography.bodyMedium
					)
					Text(
						text = recordingInfo,
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			}

		}
	}
}

@PreviewLightDark
@Composable
private fun RecordingCardNormalPreview() = RecorderAppTheme {
	RecordingCard(
		music = PreviewFakes.FAKE_VOICE_RECORDING_MODEL,
		onItemClick = {},
		onItemSelect = {},
		modifier = Modifier.fillMaxWidth()
	)
}

@PreviewLightDark
@Composable
private fun RecordingCardSelectModePreview() = RecorderAppTheme {
	RecordingCard(
		music = PreviewFakes.FAKE_VOICE_RECORDING_MODEL,
		onItemClick = {},
		onItemSelect = {},
		isSelectable = true,
		modifier = Modifier.fillMaxWidth(),
	)
}


@PreviewLightDark
@Composable
private fun RecordingCardSelectedPreview() = RecorderAppTheme {
	RecordingCard(
		music = PreviewFakes.FAKE_VOICE_RECORDING_MODEL,
		isSelectable = true,
		isSelected = true,
		onItemClick = {},
		onItemSelect = {},
		modifier = Modifier.fillMaxWidth(),
	)
}


