package com.eva.recorderapp.voice_recorder.presentation.recordings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.presentation.recordings.composable.RecordingsBottomBar
import com.eva.recorderapp.voice_recorder.presentation.recordings.composable.RecordingsInteractiveList
import com.eva.recorderapp.voice_recorder.presentation.recordings.composable.RecordingsScreenTopBar
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.RecordingScreenEvent
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.SelectableRecordings
import com.eva.recorderapp.voice_recorder.presentation.util.LocalSnackBarProvider
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsScreen(
	recordings: ImmutableList<SelectableRecordings>,
	onScreenEvent: (RecordingScreenEvent) -> Unit,
	modifier: Modifier = Modifier,
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

	Scaffold(
		topBar = {
			RecordingsScreenTopBar(
				isSelectedMode = isAnySelected,
				selectedCount = selectedCount,
				navigation = navigation,
				onUnSelectAll = { onScreenEvent(RecordingScreenEvent.OnUnSelectAllRecordings) },
				onSelectAll = { onScreenEvent(RecordingScreenEvent.OnSelectAllRecordings) },
				scrollBehavior = scrollBehavior
			)
		},
		bottomBar = {
			RecordingsBottomBar(
				recordings = recordings,
				showRename = showRenameOption,
				isVisible = isAnySelected,
				onItemDelete = { onScreenEvent(RecordingScreenEvent.OnSelectedItemTrashRequest) }
			)
		},
		snackbarHost = { SnackbarHost(hostState = snackBarProvider) },
		modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
	) { scPadding ->
		RecordingsInteractiveList(
			recordings = recordings,
			onItemClick = {},
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
		recordings = recordings,
		onScreenEvent = {},
		navigation = {
			Icon(
				imageVector = Icons.AutoMirrored.Default.ArrowBack,
				contentDescription = stringResource(R.string.back_arrow)
			)
		},
	)
}