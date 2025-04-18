package com.eva.feature_recordings.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.categories.domain.models.RecordingCategoryModel
import com.eva.feature_recordings.search.composables.SearchBarTextField
import com.eva.feature_recordings.search.composables.SearchFilterOptions
import com.eva.feature_recordings.search.composables.SearchResultsContent
import com.eva.feature_recordings.search.state.SearchRecordingScreenEvent
import com.eva.feature_recordings.search.state.SearchRecordingScreenState
import com.eva.feature_recordings.util.RecordingsPreviewFakes
import com.eva.recordings.domain.models.RecordedVoiceModel
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme
import com.eva.ui.utils.LocalSnackBarProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchRecordingsScreen(
	state: SearchRecordingScreenState,
	categories: ImmutableList<RecordingCategoryModel>,
	searchResults: ImmutableList<RecordedVoiceModel>,
	onSelectRecording: (RecordedVoiceModel) -> Unit,
	onEvent: (SearchRecordingScreenEvent) -> Unit,
	modifier: Modifier = Modifier,
	navigation: @Composable () -> Unit = {},
) {
	val snackBarProvider = LocalSnackBarProvider.current
	val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

	val scope = rememberCoroutineScope()

	var showSheet by remember { mutableStateOf(false) }
	val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

	if (showSheet) {
		ModalBottomSheet(
			sheetState = sheetState,
			onDismissRequest = { showSheet = false },
		) {
			SearchFilterOptions(
				categories = categories,
				timeFilterOption = state.timeFilter,
				selectedCategory = state.selectedCategory,
				onCategorySelect = { onEvent(SearchRecordingScreenEvent.OnCategorySelected(it)) },
				onSelectTimeFilter = { onEvent(SearchRecordingScreenEvent.OnSelectTimeFilter(it)) },
				modifier = Modifier.fillMaxWidth()
			)
		}
	}

	Scaffold(
		topBar = {
			MediumTopAppBar(
				title = { Text(text = stringResource(R.string.menu_option_search)) },
				actions = {
					TooltipBox(
						positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
						tooltip = {
							PlainTooltip {
								Text(text = stringResource(R.string.search_filter_title))
							}
						},
						state = rememberTooltipState(),
					) {
						IconButton(
							onClick = {
								scope.launch { sheetState.show() }
									.invokeOnCompletion { showSheet = true }
							},
						) {
							Icon(
								imageVector = Icons.Default.FilterList,
								contentDescription = stringResource(R.string.search_filter_title)
							)
						}
					}
				},
				navigationIcon = navigation,
				scrollBehavior = scrollBehavior,
				colors = TopAppBarDefaults
					.topAppBarColors(actionIconContentColor = MaterialTheme.colorScheme.primary)
			)
		},
		snackbarHost = { SnackbarHost(snackBarProvider) },
		modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
	) { scPadding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(scPadding)
				.padding(
					horizontal = dimensionResource(R.dimen.sc_padding),
					vertical = dimensionResource(R.dimen.sc_padding_secondary)
				),
			verticalArrangement = Arrangement.spacedBy(12.dp)
		) {
			SearchBarTextField(
				query = state.searchQuery,
				onQueryChange = { onEvent(SearchRecordingScreenEvent.OnQueryChange(it)) },
				onVoiceInput = { onEvent(SearchRecordingScreenEvent.OnVoiceSearchResults(it)) },
				modifier = Modifier.align(Alignment.CenterHorizontally)
			)
			SearchResultsContent(
				searchResults = searchResults,
				onItemSelect = onSelectRecording,
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
		categories = RecordingsPreviewFakes.FAKE_CATEGORIES_WITH_ALL_OPTION,
		searchResults = RecordingsPreviewFakes.FAKE_VOICE_RECORDINGS_SELECTED.map { it.recording }
			.toImmutableList(),
		onEvent = {},
		onSelectRecording = {},
		navigation = {
			Icon(
				imageVector = Icons.AutoMirrored.Default.ArrowBack,
				contentDescription = ""
			)
		},
	)
}