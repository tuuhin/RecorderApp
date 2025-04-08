package com.eva.feature_settings.composables.files

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
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
import com.eva.datastore.domain.enums.AudioFileNamingFormat
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme

@Composable
internal fun FileNamingFormat(
	prefix: String,
	format: AudioFileNamingFormat,
	onFormatChange: (AudioFileNamingFormat) -> Unit,
	modifier: Modifier = Modifier,
	padding: PaddingValues = PaddingValues(16.dp),
) {
	Column(
		modifier = modifier
			.wrapContentHeight()
			.padding(padding),
	) {
		Text(
			text = stringResource(id = R.string.recording_settings_file_format_title),
			style = MaterialTheme.typography.titleMedium,
			color = MaterialTheme.colorScheme.onBackground
		)
		Spacer(modifier = Modifier.height(4.dp))
		Text(
			text = stringResource(id = R.string.recording_settings_file_format_text),
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
		Column(
			verticalArrangement = Arrangement.spacedBy(2.dp)
		) {
			AudioFileNamingFormat.entries.forEach { displayFormat ->
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.clip(MaterialTheme.shapes.medium)
						.clickable(role = Role.RadioButton) { onFormatChange(displayFormat) },
					verticalAlignment = Alignment.CenterVertically
				) {
					RadioButton(
						selected = displayFormat == format,
						onClick = { onFormatChange(displayFormat) },
						colors = RadioButtonDefaults
							.colors(selectedColor = MaterialTheme.colorScheme.secondary)
					)
					Text(
						text = displayFormat.stringRes(prefix),
						style = MaterialTheme.typography.labelLarge,
						color = MaterialTheme.colorScheme.onBackground
					)
				}
			}
		}
	}
}

@PreviewLightDark
@Composable
private fun FileNamingFormatSelectorPickerPreview() = RecorderAppTheme {
	Surface {
		FileNamingFormat(
			prefix = "Voice_",
			format = AudioFileNamingFormat.COUNT,
			onFormatChange = {},
			padding = PaddingValues(10.dp)
		)
	}
}