package com.eva.feature_settings.composables.audio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.datastore.domain.enums.RecordingEncoders
import com.eva.ui.theme.RecorderAppTheme

@Composable
internal fun AudioEncoderSelector(
	encoder: RecordingEncoders,
	onEncoderChange: (RecordingEncoders) -> Unit,
	modifier: Modifier = Modifier,
	padding: PaddingValues = PaddingValues.Zero,
) {
	Column(
		modifier = modifier.padding(padding),
		verticalArrangement = Arrangement.spacedBy(4.dp),
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

				Column(
					verticalArrangement = Arrangement.spacedBy(2.dp),
					modifier = Modifier.weight(1f)
				) {
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