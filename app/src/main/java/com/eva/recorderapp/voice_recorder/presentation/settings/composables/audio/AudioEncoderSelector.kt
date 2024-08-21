package com.eva.recorderapp.voice_recorder.presentation.settings.composables.audio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecordingEncoders

@Composable
fun AudioEncoderSelector(
	encoder: RecordingEncoders,
	onEncoderChange: (RecordingEncoders) -> Unit,
	modifier: Modifier = Modifier,
	padding: PaddingValues = PaddingValues(horizontal = 12.dp)
) {
	Column(
		modifier = modifier.padding(padding),
	) {
		Text(
			text = stringResource(id = R.string.recording_settings_encoder_title),
			style = MaterialTheme.typography.titleMedium,
			color = MaterialTheme.colorScheme.onBackground
		)
		Spacer(modifier = Modifier.height(4.dp))
		Text(
			text = stringResource(id = R.string.recording_settings_encoder_text),
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
		Column(
			verticalArrangement = Arrangement.spacedBy(2.dp)
		) {
			RecordingEncoders.entries.forEach { entry ->
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.clip(MaterialTheme.shapes.medium)
						.clickable(role = Role.RadioButton, onClick = { onEncoderChange(entry) }),
					verticalAlignment = Alignment.CenterVertically
				) {
					RadioButton(
						selected = entry == encoder,
						onClick = { onEncoderChange(entry) },
						colors = RadioButtonDefaults
							.colors(selectedColor = MaterialTheme.colorScheme.secondary),
					)

					Column {
						Text(
							text = entry.titleStrRes,
							style = MaterialTheme.typography.labelLarge
						)
						Text(
							text = entry.descriptionStrRes,
							style = MaterialTheme.typography.labelSmall,
							color = MaterialTheme.colorScheme.onSurfaceVariant
						)
					}
				}
			}
		}
	}
}

@PreviewLightDark
@Composable
private fun AudioEncoderSelectorPreview() = RecorderAppTheme {
	Surface {
		AudioEncoderSelector(
			encoder = RecordingEncoders.ACC,
			onEncoderChange = {}
		)
	}
}