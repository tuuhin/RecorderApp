package com.eva.recorderapp.voice_recorder.presentation.settings.composables.audio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.datastore.enums.RecordQuality

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioQualitySelector(
	quality: RecordQuality,
	onQualityChanged: (RecordQuality) -> Unit,
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp)
) {

	Column(
		modifier = modifier
			.wrapContentHeight()
			.padding(contentPadding),
		verticalArrangement = Arrangement.spacedBy(4.dp)
	) {
		Text(
			text = stringResource(id = R.string.recording_settings_quality_title),
			style = MaterialTheme.typography.titleMedium,
			color = MaterialTheme.colorScheme.onBackground
		)
		Text(
			text = stringResource(id = R.string.recording_settings_quality_text),
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
		Spacer(modifier = Modifier.height(2.dp))

		SingleChoiceSegmentedButtonRow(
			modifier = Modifier.fillMaxWidth()
		) {

			RecordQuality.entries.forEachIndexed { idx, entry ->
				SegmentedButton(
					selected = quality == entry,
					onClick = { onQualityChanged(entry) },
					shape = SegmentedButtonDefaults.itemShape(idx, RecordQuality.entries.size),
					label = {
						TooltipBox(
							positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
							tooltip = {
								PlainTooltip(
									shape = MaterialTheme.shapes.small,
									tonalElevation = 2.dp,
									shadowElevation = 4.dp
								) {
									Text(
										text = stringResource(
											id = R.string.audio_quality_sample_rate_bit_rate,
											entry.sampleRateInKhz,
											entry.bitRateInKbps
										),
										style = MaterialTheme.typography.labelSmall
									)
								}
							},
							state = rememberTooltipState()
						) {
							Text(text = entry.strRes)
						}
					}
				)
			}
		}
	}
}

@PreviewLightDark
@Composable
private fun AudioQualitySelectorPreview() = RecorderAppTheme {
	Surface {
		AudioQualitySelector(
			quality = RecordQuality.NORMAL,
			onQualityChanged = {},
		)
	}
}