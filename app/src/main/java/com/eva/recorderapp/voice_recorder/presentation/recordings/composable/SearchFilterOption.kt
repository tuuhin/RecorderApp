package com.eva.recorderapp.voice_recorder.presentation.recordings.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowOverflow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.categories.models.RecordingCategoryModel
import com.eva.recorderapp.voice_recorder.presentation.recordings.search.SearchFilterTimeOption
import com.eva.recorderapp.voice_recorder.presentation.util.CategoryImmutableList
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchFilterOptions(
	categories: CategoryImmutableList,
	onCategorySelect: (RecordingCategoryModel?) -> Unit,
	onSelectTimeFilter: (SearchFilterTimeOption?) -> Unit,
	timeFilterOption: SearchFilterTimeOption? = null,
	selectedCategory: RecordingCategoryModel? = null,
	modifier: Modifier = Modifier,
) {

	var showOptions by rememberSaveable { mutableStateOf(false) }
	val isInspectionMode = LocalInspectionMode.current

	val rotateAngles by animateFloatAsState(
		targetValue = if (showOptions) 0f else 360f,
		animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
	)

	Column(modifier = modifier) {
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				text = stringResource(R.string.search_filter_title),
				style = MaterialTheme.typography.titleMedium,
				color = MaterialTheme.colorScheme.onSurface,
			)
			IconButton(
				onClick = { showOptions = !showOptions },
				colors = IconButtonDefaults
					.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
			) {
				Icon(
					imageVector = Icons.Default.ArrowDropDown,
					contentDescription = "Filter Options",
					modifier = Modifier.graphicsLayer {
						rotationZ = rotateAngles
					},
				)
			}
		}
		AnimatedVisibility(
			visible = isInspectionMode || showOptions,
			enter = expandVertically(),
			exit = shrinkVertically()
		) {
			ElevatedCard(
				shape = MaterialTheme.shapes.medium,
				modifier = Modifier.fillMaxWidth()
			) {
				Column(
					modifier = Modifier.padding(dimensionResource(R.dimen.card_padding))
				) {
					Text(
						text = stringResource(R.string.search_filter_date_created_title),
						style = MaterialTheme.typography.titleSmall
					)
					FlowRow(
						horizontalArrangement = Arrangement.spacedBy(8.dp),
						maxItemsInEachRow = 3,
						overflow = FlowRowOverflow.Visible
					) {
						SearchFilterTimeOption.entries.forEach { timeFilter ->
							InputChip(
								selected = timeFilterOption == timeFilter,
								onClick = {
									val selection = if (timeFilterOption == timeFilter)
										timeFilter else null
									onSelectTimeFilter(selection)
								},
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
						maxItemsInEachRow = 3,
						overflow = FlowRowOverflow.Visible
					) {
						categories.forEach { category ->
							InputChip(
								selected = selectedCategory == category,
								onClick = {
									val selection = if (selectedCategory == category)
										category else null
									onCategorySelect(selection)
								},
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
}


@PreviewLightDark
@Composable
private fun SearchFilterOptionsPreview() = RecorderAppTheme {
	Surface {
		SearchFilterOptions(
			categories = PreviewFakes.FAKE_CATEGORIES_WITH_ALL_OPTION,
			onCategorySelect = {},
			onSelectTimeFilter = {}
		)
	}
}