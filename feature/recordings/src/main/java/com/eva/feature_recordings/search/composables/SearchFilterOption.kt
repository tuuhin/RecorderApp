package com.eva.feature_recordings.search.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowOverflow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.categories.domain.models.RecordingCategoryModel
import com.eva.feature_recordings.search.state.SearchFilterTimeOption
import com.eva.feature_recordings.util.RecordingsPreviewFakes
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SearchFilterOptions(
	categories: ImmutableList<RecordingCategoryModel>,
	onCategorySelect: (RecordingCategoryModel?) -> Unit,
	onSelectTimeFilter: (SearchFilterTimeOption?) -> Unit,
	modifier: Modifier = Modifier,
	timeFilterOption: SearchFilterTimeOption? = null,
	selectedCategory: RecordingCategoryModel? = null,
) {

	Column(
		modifier = modifier.padding(dimensionResource(R.dimen.sc_padding))
	) {
		Text(
			text = stringResource(R.string.search_filter_title),
			style = MaterialTheme.typography.titleMedium,
			color = MaterialTheme.colorScheme.secondary
		)
		Spacer(modifier = Modifier.height(6.dp))

		ElevatedCard(
			shape = MaterialTheme.shapes.medium,
			modifier = Modifier.fillMaxWidth(),
			colors = CardDefaults.elevatedCardColors(
				containerColor = MaterialTheme.colorScheme.surfaceContainer,
				contentColor = contentColorFor(MaterialTheme.colorScheme.surfaceContainer)
			)
		) {
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(dimensionResource(R.dimen.card_padding))
			) {
				Text(
					text = stringResource(R.string.search_filter_date_created_title),
					style = MaterialTheme.typography.titleSmall
				)
				FlowRow(
					horizontalArrangement = Arrangement.spacedBy(8.dp),
					maxItemsInEachRow = 5,
					overflow = FlowRowOverflow.Visible
				) {
					SearchFilterTimeOption.entries.forEach { timeFilter ->
						InputChip(
							selected = timeFilterOption == timeFilter,
							onClick = { onSelectTimeFilter(timeFilter) },
							label = {
								Text(
									text = timeFilter.strRes,
									style = MaterialTheme.typography.bodySmall
								)
							},
							shape = MaterialTheme.shapes.medium,
							colors = InputChipDefaults.inputChipColors(selectedLabelColor = MaterialTheme.colorScheme.secondary)
						)
					}
				}
				Spacer(modifier = Modifier.height(6.dp))
				Text(
					text = stringResource(R.string.search_filter_categories_title),
					style = MaterialTheme.typography.titleSmall
				)
				FlowRow(
					horizontalArrangement = Arrangement.spacedBy(8.dp),
					maxItemsInEachRow = 5,
					overflow = FlowRowOverflow.Visible
				) {
					categories.forEach { category ->
						InputChip(
							selected = selectedCategory == category,
							onClick = { onCategorySelect(category) },
							label = {
								Text(
									text = category.name,
									style = MaterialTheme.typography.bodyMedium
								)
							},

							shape = MaterialTheme.shapes.medium,
							colors = InputChipDefaults.inputChipColors(selectedLabelColor = MaterialTheme.colorScheme.secondary)
						)
					}
				}
			}
		}
	}

}


@PreviewLightDark
@Composable
private fun SearchFilterOptionsPreview() = RecorderAppTheme {
	Surface {
		SearchFilterOptions(
			categories = RecordingsPreviewFakes.FAKE_CATEGORIES_WITH_ALL_OPTION,
			onCategorySelect = {},
			onSelectTimeFilter = {}
		)
	}
}