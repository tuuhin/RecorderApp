package com.eva.recorderapp.voice_recorder.presentation.recordings.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.presentation.recordings.composable.SearchResultsContent
import com.eva.recorderapp.voice_recorder.presentation.util.CategoryImmutableList
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import com.eva.recorderapp.voice_recorder.presentation.util.RecordedVoiceModelsList
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchRecordingsScreen(
	state: SearchRecordingScreenState,
	categories: CategoryImmutableList,
	searchResults: RecordedVoiceModelsList,
	onSelectRecording: (RecordedVoiceModel) -> Unit,
	onEvent: (SearchRecordingScreenEvent) -> Unit,
	modifier: Modifier = Modifier,
	navigation: @Composable () -> Unit = {},
) {
	Box(modifier = modifier.fillMaxSize()) {
		SearchBar(
			inputField = {
				SearchBarDefaults.InputField(
					query = state.searchQuery,
					onQueryChange = { onEvent(SearchRecordingScreenEvent.OnQueryChange(it)) },
					onSearch = { },
					expanded = true,
					onExpandedChange = { },
					placeholder = { Text(text = stringResource(R.string.search_bar_placeholder)) },
					leadingIcon = navigation,
				)
			},
			expanded = true,
			onExpandedChange = { },
			colors = SearchBarDefaults.colors(
				containerColor = MaterialTheme.colorScheme.surface,
				dividerColor = MaterialTheme.colorScheme.outlineVariant
			)
		) {
			SearchResultsContent(
				categories = categories,
				searchResults = searchResults,
				timeFilterOption = state.timeFilter,
				selectedCategory = state.selectedCategory,
				onItemSelect = onSelectRecording,
				onCategorySelect = { onEvent(SearchRecordingScreenEvent.OnCategorySelected(it)) },
				onTimeFilterSelect = { onEvent(SearchRecordingScreenEvent.OnSelectTimeFilter(it)) },
				contentPadding = PaddingValues(
					horizontal = dimensionResource(R.dimen.sc_padding),
					vertical = dimensionResource(R.dimen.sc_padding_secondary)
				),
				modifier = Modifier.fillMaxSize(),
			)
		}
	}
}


@PreviewLightDark
@Composable
private fun SearchRecordingsScreenPreview() = RecorderAppTheme {
	SearchRecordingsScreen(
		state = SearchRecordingScreenState(),
		categories = PreviewFakes.FAKE_CATEGORIES_WITH_ALL_OPTION,
		searchResults = PreviewFakes.FAKE_VOICE_RECORDINGS_SELECTED.map { it.recoding }
			.toImmutableList(),
		onEvent = {},
		onSelectRecording = {},
		navigation = {
			Icon(
				imageVector = Icons.AutoMirrored.Default.ArrowBack,
				contentDescription = stringResource(R.string.back_arrow)
			)
		},
	)
}