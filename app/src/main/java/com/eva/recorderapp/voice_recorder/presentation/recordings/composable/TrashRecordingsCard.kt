package com.eva.recorderapp.voice_recorder.presentation.recordings.composable

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import com.eva.recorderapp.common.LocalTimeFormats
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.recordings.models.TrashRecordingModel
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import kotlinx.datetime.format

@Composable
fun TrashRecordingsCard(
	trashRecording: TrashRecordingModel,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	isSelectable: Boolean = false,
	isSelected: Boolean = false,
	shape: Shape = MaterialTheme.shapes.large,
) {

	val context = LocalContext.current

	val expiryDateText = remember(trashRecording.expiresAt) {
		val readbleText =
			trashRecording.expiresAt.format(LocalTimeFormats.RECORDING_RECORD_TIME_FORMAT)
		context.getString(R.string.recording_info_expires_at, readbleText)
	}

	ElevatedCard(
		shape = shape,
		elevation = CardDefaults.elevatedCardElevation(pressedElevation = 4.dp),
		modifier = modifier
			.clip(shape)
			.clickable(onClick = onClick),
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
						onClick = onClick,
						colors = RadioButtonDefaults
							.colors(selectedColor = MaterialTheme.colorScheme.secondary)

					)
				} else {
					Image(
						painter = painterResource(id = R.drawable.ic_record_circle),
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
					text = trashRecording.displayName,
					style = MaterialTheme.typography.titleMedium,
					color = MaterialTheme.colorScheme.primary
				)
				Text(
					text = expiryDateText,
					style = MaterialTheme.typography.bodySmall,
					modifier = Modifier.align(Alignment.End)
				)

			}

		}
	}
}


@PreviewLightDark
@Composable
private fun TrashRecordingsCardPreview() = RecorderAppTheme {
	TrashRecordingsCard(
		trashRecording = PreviewFakes.FAKE_TRASH_RECORDINGS_MODEL,
		onClick = { },
	)
}