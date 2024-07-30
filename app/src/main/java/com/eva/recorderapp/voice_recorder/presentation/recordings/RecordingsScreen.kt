package com.eva.recorderapp.voice_recorder.presentation.recordings

import androidx.compose.foundation.layout.PaddingValues
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
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.presentation.recordings.composable.RecordingsBottomBar
import com.eva.recorderapp.voice_recorder.presentation.recordings.composable.RecordingsInteractiveList
import com.eva.recorderapp.voice_recorder.presentation.recordings.composable.RecordingsScreenTopBar
import com.eva.recorderapp.voice_recorder.presentation.recordings.composable.RenameRecordingsNameDialog
import com.eva.recorderapp.voice_recorder.presentation.recordings.composable.SortOptionsSheetContent
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.event.RecordingScreenEvent
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.event.RenameRecordingEvents
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.RecordingsSortInfo
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.RenameRecordingState
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.SelectableRecordings
import com.eva.recorderapp.voice_recorder.presentation.util.LocalSnackBarProvider
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsScreen(
	isRecordingsLoaded: Boolean,
	sortInfo: RecordingsSortInfo,
	recordings: ImmutableList<SelectableRecordings>,
	renameState: RenameRecordingState,
	onRenameEvent: (RenameRecordingEvents) -> Unit,
	onScreenEvent: (RecordingScreenEvent) -> Unit,
	onRecordingSelect: (RecordedVoiceModel) -> Unit,
	modifier: Modifier = Modifier,
	onNavigateToBin: () -> Unit = {},
	navigation: @Composable () -> Unit = {},
) {
	val snackBarProvider = LocalSnackBarProvider.current
	val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

	val isAnySelected by remember(recordings) {
		derivedStateOf { recordings.any { it.isSelected } }
	}

	val selectedCount by remember(recordings, isAnySelected) {
		derivedStateOf {
			if (!isAnySelected) return@derivedStateOf 0
			recordings.filter { it.isSelected }.count()
		}
	}

	val showRenameOption by remember(selectedCount) {
		derivedStateOf { selectedCount == 1 }
	}

	var showSheet by remember { mutableStateOf(false) }
	val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

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

	RenameRecordingsNameDialog(
		state = renameState,
		onEvent = onRenameEvent
	)

	Scaffold(
		topBar = {
			RecordingsScreenTopBar(
				isSelectedMode = isAnySelected,
				selectedCount = selectedCount,
				navigation = navigation,
				onUnSelectAll = { onScreenEvent(RecordingScreenEvent.OnUnSelectAllRecordings) },
				onSelectAll = { onScreenEvent(RecordingScreenEvent.OnSelectAllRecordings) },
				onSortItems = {
					scope.launch {
						sheetState.show()
					}.invokeOnCompletion {
						showSheet = true
					}
				},

				onNavigateToBin = onNavigateToBin,
				scrollBehavior = scrollBehavior
			)
		},
		bottomBar = {
			RecordingsBottomBar(
				recordings = recordings,
				showRename = showRenameOption,
				isVisible = isAnySelected,
				onShareSelected = { onScreenEvent(RecordingScreenEvent.ShareSelectedRecordings) },
				onItemDelete = { onScreenEvent(RecordingScreenEvent.OnSelectedItemTrashRequest) },
				onRename = { onRenameEvent(RenameRecordingEvents.OnShowRenameDialog) }
			)
		},
		snackbarHost = { SnackbarHost(hostState = snackBarProvider) },
		modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
	) { scPadding ->
		RecordingsInteractiveList(
			isRecordingsLoaded = isRecordingsLoaded,
			recordings = recordings,
			onItemClick = onRecordingSelect,
			onItemSelect = { record ->
				onScreenEvent(RecordingScreenEvent.OnRecordingSelectOrUnSelect(record))
			},
			contentPadding = PaddingValues(
				start = dimensionResource(id = R.dimen.sc_padding),
				end = dimensionResource(R.dimen.sc_padding),
				top = dimensionResource(id = R.dimen.sc_padding_secondary) + scPadding.calculateTopPadding(),
				bottom = dimensionResource(id = R.dimen.sc_padding_secondary) + scPadding.calculateBottomPadding()
			),
		)
	}
}


class SelectedRecordingsPreviewParams :
	CollectionPreviewParameterProvider<ImmutableList<SelectableRecordings>>(
		listOf(
			persistentListOf(),
			PreviewFakes.FAKE_VOICE_RECORDING_MODELS,
			PreviewFakes.FAKE_VOICE_RECORDINGS_SELECTED
		)
	)

@PreviewLightDark
@Composable
private fun RecordingScreenPreview(
	@PreviewParameter(SelectedRecordingsPreviewParams::class)
	recordings: ImmutableList<SelectableRecordings>
) = RecorderAppTheme {
	RecordingsScreen(
		isRecordingsLoaded = true,
		recordings = recordings,
		sortInfo = RecordingsSortInfo(),
		renameState = RenameRecordingState(),
		onScreenEvent = {},
		onRenameEvent = {},
		onRecordingSelect = {},
		navigation = {
			Icon(
				imageVector = Icons.AutoMirrored.Default.ArrowBack,
				contentDescription = stringResource(R.string.back_arrow)
			)
		},
	)
}