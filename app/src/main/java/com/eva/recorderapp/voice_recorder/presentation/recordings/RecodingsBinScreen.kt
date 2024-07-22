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
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.presentation.recordings.composable.RecordingBinScreenTopBar
import com.eva.recorderapp.voice_recorder.presentation.recordings.composable.RecordingsBinScreenBottomBar
import com.eva.recorderapp.voice_recorder.presentation.recordings.composable.RecordingsInteractiveList
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.SelectableRecordings
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.TrashScreenEvent
import com.eva.recorderapp.voice_recorder.presentation.util.LocalSnackBarProvider
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsBinScreen(
	recordings: ImmutableList<SelectableRecordings>,
	onScreenEvent: (TrashScreenEvent) -> Unit,
	modifier: Modifier = Modifier,
	navigation: @Composable () -> Unit = {}
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

	Scaffold(
		topBar = {
			RecordingBinScreenTopBar(
				isSelectedMode = isAnySelected,
				selectedCount = selectedCount,
				onUnSelectAll = { onScreenEvent(TrashScreenEvent.OnUnSelectTrashRecording) },
				onSelectAll = { onScreenEvent(TrashScreenEvent.OnSelectTrashRecording) },
				navigation = navigation
			)
		},
		bottomBar = {
			RecordingsBinScreenBottomBar(
				recordings = recordings,
				isVisible = isAnySelected,
				onItemsDelete = { onScreenEvent(TrashScreenEvent.OnSelectItemDeleteForeEver) },
				onItemsRestore = { onScreenEvent(TrashScreenEvent.OnSelectItemRestore) },
			)
		},
		snackbarHost = { SnackbarHost(hostState = snackBarProvider) },
		modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
	) { scPadding ->
		RecordingsInteractiveList(
			recordings = recordings,
			onItemClick = { record ->
				onScreenEvent(TrashScreenEvent.OnRecordingSelectOrUnSelect(record))
			},
			onItemSelect = { record ->
				onScreenEvent(TrashScreenEvent.OnRecordingSelectOrUnSelect(record))
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

@PreviewLightDark
@Composable
private fun RecordingsBinScreenPreview(
	@PreviewParameter(SelectedRecordingsPreviewParams::class)
	recordings: ImmutableList<SelectableRecordings>
) = RecorderAppTheme {
	RecordingsBinScreen(
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