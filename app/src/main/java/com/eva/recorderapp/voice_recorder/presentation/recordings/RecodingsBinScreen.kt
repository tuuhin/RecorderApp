package com.eva.recorderapp.voice_recorder.presentation.recordings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import com.eva.recorderapp.voice_recorder.presentation.recordings.composable.RecordingBinScreenTopBar
import com.eva.recorderapp.voice_recorder.presentation.recordings.composable.RecordingsBinScreenBottomBar
import com.eva.recorderapp.voice_recorder.presentation.recordings.composable.RecordingsInteractiveList
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.event.TrashRecordingScreenEvent
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.SelectableTrashRecordings
import com.eva.recorderapp.voice_recorder.presentation.util.LocalSnackBarProvider
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsBinScreen(
	isRecordingsLoaded: Boolean,
	recordings: ImmutableList<SelectableTrashRecordings>,
	onScreenEvent: (TrashRecordingScreenEvent) -> Unit,
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
				navigation = navigation,
				onUnSelectAll = { onScreenEvent(TrashRecordingScreenEvent.OnUnSelectTrashRecording) },
				onSelectAll = { onScreenEvent(TrashRecordingScreenEvent.OnSelectTrashRecording) },
			)
		},
		bottomBar = {
			RecordingsBinScreenBottomBar(
				recordings = recordings,
				isVisible = isAnySelected,
				onItemsDelete = { onScreenEvent(TrashRecordingScreenEvent.OnSelectItemDeleteForeEver) },
				onItemsRestore = { onScreenEvent(TrashRecordingScreenEvent.OnSelectItemRestore) },
			)
		},
		snackbarHost = { SnackbarHost(hostState = snackBarProvider) },
		modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
	) { scPadding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(
					paddingValues = PaddingValues(
						start = dimensionResource(id = R.dimen.sc_padding),
						end = dimensionResource(R.dimen.sc_padding),
						top = dimensionResource(id = R.dimen.sc_padding_secondary) + scPadding.calculateTopPadding(),
						bottom = dimensionResource(id = R.dimen.sc_padding_secondary) + scPadding.calculateBottomPadding()
					)
				), verticalArrangement = Arrangement.spacedBy(4.dp)
		) {
			Text(
				text = stringResource(R.string.recording_bin_explainantion),
				modifier = Modifier
					.align(Alignment.CenterHorizontally)
					.padding(vertical = 2.dp),
				style = MaterialTheme.typography.labelMedium,
			)
			RecordingsInteractiveList(
				isRecordingsLoaded = isRecordingsLoaded,
				recordings = recordings,
				onItemSelect = { record ->
					onScreenEvent(TrashRecordingScreenEvent.OnRecordingSelectOrUnSelect(record))
				}
			)
		}
	}
}

class SelectableTrashRecordingPreviewParams :
	CollectionPreviewParameterProvider<ImmutableList<SelectableTrashRecordings>>(
		listOf(
			PreviewFakes.FAKE_TRASH_RECORDINGS_MODELS
		)
	)

@PreviewLightDark
@Composable
private fun RecordingsBinScreenPreview(
	@PreviewParameter(SelectableTrashRecordingPreviewParams::class)
	recordings: ImmutableList<SelectableTrashRecordings>
) = RecorderAppTheme {
	RecordingsBinScreen(
		isRecordingsLoaded = true,
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