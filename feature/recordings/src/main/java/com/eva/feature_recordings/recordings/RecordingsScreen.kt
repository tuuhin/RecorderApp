package com.eva.feature_recordings.recordings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.eva.categories.domain.models.RecordingCategoryModel
import com.eva.feature_recordings.composable.MediaAccessPermissionWrapper
import com.eva.feature_recordings.recordings.composables.RecordingsBottomBar
import com.eva.feature_recordings.recordings.composables.RecordingsInteractiveList
import com.eva.feature_recordings.recordings.composables.RecordingsScreenTopBar
import com.eva.feature_recordings.recordings.composables.SortOptionsSheetContent
import com.eva.feature_recordings.recordings.state.RecordingScreenEvent
import com.eva.feature_recordings.recordings.state.RecordingsSortInfo
import com.eva.feature_recordings.recordings.state.SelectableRecordings
import com.eva.feature_recordings.util.RecordingsPreviewFakes
import com.eva.recordings.domain.models.RecordedVoiceModel
import com.eva.ui.R
import com.eva.ui.animation.SharedElementTransitionKeys
import com.eva.ui.animation.sharedBoundsWrapper
import com.eva.ui.theme.RecorderAppTheme
import com.eva.ui.utils.LocalSnackBarProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun RecordingsScreen(
	isRecordingsLoaded: Boolean,
	selectedCategory: RecordingCategoryModel,
	sortInfo: RecordingsSortInfo,
	recordings: ImmutableList<SelectableRecordings>,
	categories: ImmutableList<RecordingCategoryModel>,
	onScreenEvent: (RecordingScreenEvent) -> Unit,
	onRecordingSelect: (RecordedVoiceModel) -> Unit,
	modifier: Modifier = Modifier,
	onNavigateToBin: () -> Unit = {},
	onShowRenameDialog: (RecordedVoiceModel?) -> Unit = {},
	onMoveToCategory: (Collection<RecordedVoiceModel>) -> Unit = {},
	onNavigationToCategories: () -> Unit = {},
	onNavigateToSearch: () -> Unit = {},
	navigation: @Composable () -> Unit = {},
) {
	val snackBarProvider = LocalSnackBarProvider.current
	val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

	val isAnySelected by remember(recordings) {
		derivedStateOf { recordings.any(SelectableRecordings::isSelected) }
	}

	val selectedCount by remember(recordings) {
		derivedStateOf {
			recordings.count(SelectableRecordings::isSelected)
		}
	}

	val showRenameOption by remember(selectedCount) {
		derivedStateOf { selectedCount == 1 }
	}

	var showSheet by remember { mutableStateOf(false) }
	val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

	val scope = rememberCoroutineScope()

	if (showSheet) {
		ModalBottomSheet(
			sheetState = sheetState,
			onDismissRequest = { showSheet = false },
			tonalElevation = 2.dp
		) {
			SortOptionsSheetContent(
				sortInfo = sortInfo,
				onSortTypeChange = { onScreenEvent(RecordingScreenEvent.OnSortOptionChange(it)) },
				onSortOrderChange = { onScreenEvent(RecordingScreenEvent.OnSortOrderChange(it)) },
			)
		}
	}

	BackHandler(
		enabled = isAnySelected,
		onBack = { onScreenEvent(RecordingScreenEvent.OnUnSelectAllRecordings) },
	)

	Scaffold(
		topBar = {
			RecordingsScreenTopBar(
				isSelectedMode = isAnySelected,
				selectedCount = selectedCount,
				navigation = navigation,
				onManageCategories = onNavigationToCategories,
				onNavigateToSearch = onNavigateToSearch,
				onUnSelectAll = { onScreenEvent(RecordingScreenEvent.OnUnSelectAllRecordings) },
				onSelectAll = { onScreenEvent(RecordingScreenEvent.OnSelectAllRecordings) },
				onSortItems = {
					scope.launch { sheetState.show() }
						.invokeOnCompletion {
							showSheet = true
						}
				},
				onNavigateToBin = onNavigateToBin,
				scrollBehavior = scrollBehavior
			)
		},
		bottomBar = {
			RecordingsBottomBar(
				showRename = showRenameOption,
				isVisible = isAnySelected,
				onShareSelected = { onScreenEvent(RecordingScreenEvent.ShareSelectedRecordings) },
				onItemDelete = { onScreenEvent(RecordingScreenEvent.OnSelectedItemTrashRequest) },
				onStarItem = { onScreenEvent(RecordingScreenEvent.OnToggleFavourites) },
				onRename = {
					// no rename option if option is not available
					if (showRenameOption) {
						val firstSelected = recordings.filter { it.isSelected }
							.map { it.recording }.firstOrNull()
						// show the rename dialog
						onShowRenameDialog(firstSelected)
					}
				},
				onMoveToCategory = {
					val selectedOnes = recordings.filter { it.isSelected }
						.map { it.recording }
					onMoveToCategory(selectedOnes)
				},
			)
		},
		snackbarHost = { SnackbarHost(hostState = snackBarProvider) },
		modifier = modifier
			.nestedScroll(scrollBehavior.nestedScrollConnection)
			.sharedBoundsWrapper(key = SharedElementTransitionKeys.RECORDINGS_LIST_SHARED_BOUNDS),
	) { scPadding ->
		MediaAccessPermissionWrapper(
			onLoadRecordings = { onScreenEvent(RecordingScreenEvent.PopulateRecordings) },
			modifier = Modifier.padding(scPadding)
		) {
			RecordingsInteractiveList(
				isRecordingsLoaded = isRecordingsLoaded,
				selectedCategory = selectedCategory,
				recordings = recordings,
				categories = categories,
				onItemClick = onRecordingSelect,
				onCategorySelect = { category ->
					onScreenEvent(RecordingScreenEvent.OnCategoryChanged(category))
				},
				onItemSelect = { record ->
					onScreenEvent(RecordingScreenEvent.OnRecordingSelectOrUnSelect(record))
				},
				contentPadding = PaddingValues(
					horizontal = dimensionResource(id = R.dimen.sc_padding),
					vertical = dimensionResource(id = R.dimen.sc_padding_secondary)
				),
				modifier = Modifier.fillMaxSize(),
			)
		}
	}
}


private class SelectedRecordingsPreviewParams :
	CollectionPreviewParameterProvider<ImmutableList<SelectableRecordings>>(
		listOf(
			RecordingsPreviewFakes.FAKE_VOICE_RECORDINGS_EMPTY,
			RecordingsPreviewFakes.FAKE_VOICE_RECORDING_MODELS,
			RecordingsPreviewFakes.FAKE_VOICE_RECORDINGS_SELECTED
		)
	)

@PreviewLightDark
@Composable
private fun RecordingScreenPreview(
	@PreviewParameter(SelectedRecordingsPreviewParams::class)
	recordings: ImmutableList<SelectableRecordings>,
) = RecorderAppTheme {
	RecordingsScreen(
		isRecordingsLoaded = true,
		recordings = recordings,
		selectedCategory = RecordingCategoryModel.ALL_CATEGORY,
		categories = RecordingsPreviewFakes.FAKE_CATEGORIES_WITH_ALL_OPTION,
		sortInfo = RecordingsSortInfo(),
		onScreenEvent = {},
		onRecordingSelect = {},
		navigation = {
			Icon(
				imageVector = Icons.AutoMirrored.Default.ArrowBack,
				contentDescription = stringResource(R.string.back_arrow)
			)
		},
	)
}