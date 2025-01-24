package com.eva.recorderapp.voice_recorder.presentation.recordings.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.RecordingsSortInfo
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.SortOptions
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.SortOrder

@Composable
fun SortOptionsSheetContent(
	sortInfo: RecordingsSortInfo,
	onSortTypeChange: (SortOptions) -> Unit,
	onSortOrderChange: (SortOrder) -> Unit,
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(12.dp),
) {
	Column(
		modifier = modifier.padding(contentPadding),
		verticalArrangement = Arrangement.spacedBy(4.dp)
	) {
		Text(
			text = stringResource(id = R.string.sort_options_title),
			fontWeight = FontWeight.Bold,
			style = MaterialTheme.typography.titleMedium
		)
		SortOptions.entries.forEach { option ->
			Row(
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier
					.fillMaxWidth()
					.clip(MaterialTheme.shapes.medium)
					.clickable { onSortTypeChange(option) }
			) {
				Text(
					text = option.strRes,
					style = MaterialTheme.typography.bodyMedium,
					modifier = Modifier.weight(1f),
				)
				RadioButton(
					selected = sortInfo.options == option,
					onClick = { onSortTypeChange(option) },
					colors = RadioButtonDefaults
						.colors(selectedColor = MaterialTheme.colorScheme.secondary),
					modifier = Modifier.size(40.dp)
				)
			}
		}
		HorizontalDivider(
			color = MaterialTheme.colorScheme.outlineVariant,
			modifier = Modifier.padding(vertical = 4.dp)
		)
		Text(
			text = stringResource(id = R.string.sort_order_title),
			style = MaterialTheme.typography.titleMedium,
		)
		SortOrder.entries.forEach { order ->
			Row(
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier
					.fillMaxWidth()
					.clip(MaterialTheme.shapes.medium)
					.clickable { onSortOrderChange(order) }
			) {
				Text(
					text = order.strResource,
					style = MaterialTheme.typography.bodyMedium,
					modifier = Modifier.weight(1f)
				)
				RadioButton(
					selected = sortInfo.order == order,
					onClick = { onSortOrderChange(order) },
					colors = RadioButtonDefaults
						.colors(selectedColor = MaterialTheme.colorScheme.secondary),
					modifier = Modifier.size(40.dp)
				)
			}
		}
	}
}

@PreviewLightDark
@Composable
private fun SortOptionsSheetContentPreview() = RecorderAppTheme {
	Surface {
		SortOptionsSheetContent(
			sortInfo = RecordingsSortInfo(),
			onSortTypeChange = {},
			onSortOrderChange = {},
			modifier = Modifier.fillMaxWidth()
		)
	}
}