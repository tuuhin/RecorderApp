package com.eva.recorderapp.voice_recorder.presentation.recordings.composable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.categories.models.RecordingCategoryModel
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.presentation.recordings.search.SearchFilterTimeOption
import com.eva.recorderapp.voice_recorder.presentation.util.CategoryImmutableList
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import com.eva.recorderapp.voice_recorder.presentation.util.RecordedVoiceModelsList
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchResultsContent(
	categories: CategoryImmutableList,
	searchResults: RecordedVoiceModelsList,
	onCategorySelect: (RecordingCategoryModel?) -> Unit,
	onTimeFilterSelect: (SearchFilterTimeOption?) -> Unit,
	onItemSelect: (RecordedVoiceModel) -> Unit,
	timeFilterOption: SearchFilterTimeOption? = null,
	selectedCategory: RecordingCategoryModel? = null,
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(),
) {

	val isLocalInspectionMode = LocalInspectionMode.current

	val keys: ((Int, RecordedVoiceModel) -> Any)? = remember {
		if (isLocalInspectionMode) null
		else { _, recording -> recording.id }
	}

	val contentType: ((Int, RecordedVoiceModel) -> Any?) = remember {
		{ _, _ -> RecordedVoiceModel::class.simpleName }
	}

	LazyColumn(
		contentPadding = contentPadding,
		modifier = modifier,
		verticalArrangement = Arrangement.spacedBy(6.dp)
	) {
		stickyHeader {
			SearchFilterOptions(
				categories = categories,
				timeFilterOption = timeFilterOption,
				selectedCategory = selectedCategory,
				onCategorySelect = onCategorySelect,
				onSelectTimeFilter = onTimeFilterSelect,
			)
		}
		itemsIndexed(
			items = searchResults,
			key = keys,
			contentType = contentType
		) { _, record ->
			RecordingCard(
				music = record,
				isSelected = false,
				isSelectable = false,
				onItemClick = { onItemSelect(record) },
				onItemSelect = { },
				modifier = Modifier
					.fillMaxWidth()
					.animateItem(),
			)
		}
	}
}

@PreviewLightDark
@Composable
private fun SearchResultsContentPreview() = RecorderAppTheme {
	Surface {
		SearchResultsContent(
			categories = PreviewFakes.FAKE_CATEGORIES_WITH_ALL_OPTION,
			searchResults = PreviewFakes.FAKE_VOICE_RECORDINGS_SELECTED.map { it.recoding }
				.toImmutableList(),
			onCategorySelect = {},
			onItemSelect = {},
			onTimeFilterSelect = {},
			contentPadding = PaddingValues(
				horizontal = dimensionResource(R.dimen.sc_padding),
				vertical = dimensionResource(R.dimen.sc_padding_secondary)
			),
		)
	}
}