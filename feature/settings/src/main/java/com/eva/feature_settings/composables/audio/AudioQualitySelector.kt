package com.eva.feature_settings.composables.audio

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.datastore.domain.enums.RecordQuality
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AudioQualitySelector(
	quality: RecordQuality,
	onQualityChanged: (RecordQuality) -> Unit,
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues.Zero,
	selectedContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
	selectedContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
	SingleChoiceSegmentedButtonRow(
		modifier = modifier.padding(contentPadding),
	) {
		RecordQuality.entries.forEachIndexed { idx, entry ->
			SegmentedButton(
				selected = quality == entry,
				onClick = { onQualityChanged(entry) },
				shape = SegmentedButtonDefaults.itemShape(idx, RecordQuality.entries.size),
				colors = SegmentedButtonDefaults.colors(
					activeContainerColor = selectedContainerColor,
					activeContentColor = selectedContentColor,
				),
				label = {
					TooltipBox(
						positionProvider = TooltipDefaults
							.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
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
						Text(text = entry.strRes, fontWeight = FontWeight.SemiBold)
					}
				}
			)
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