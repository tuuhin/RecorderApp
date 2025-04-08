package com.eva.feature_recordings.bin

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.eva.feature_recordings.bin.composables.RecordingBinScreenTopBar
import com.eva.feature_recordings.bin.composables.RecordingsBinScreenBottomBar
import com.eva.feature_recordings.bin.composables.TrashRecordingsInteractiveList
import com.eva.feature_recordings.bin.state.SelectableTrashRecordings
import com.eva.feature_recordings.bin.state.TrashRecordingScreenEvent
import com.eva.feature_recordings.composable.MediaAccessPermissionWrapper
import com.eva.feature_recordings.util.RecordingsPreviewFakes
import com.eva.ui.R
import com.eva.ui.animation.SharedElementTransitionKeys
import com.eva.ui.animation.sharedBoundsWrapper
import com.eva.ui.theme.RecorderAppTheme
import com.eva.ui.utils.LocalSnackBarProvider
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun RecordingsBinScreen(
	isRecordingsLoaded: Boolean,
	recordings: ImmutableList<SelectableTrashRecordings>,
	onScreenEvent: (TrashRecordingScreenEvent) -> Unit,
	modifier: Modifier = Modifier,
	navigation: @Composable () -> Unit = {},
) {
	val snackBarProvider = LocalSnackBarProvider.current
	val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

	val isAnySelected by remember(recordings) {
		derivedStateOf { recordings.any(SelectableTrashRecordings::isSelected) }
	}

	val selectedCount by remember(recordings) {
		derivedStateOf {
			recordings.count(SelectableTrashRecordings::isSelected)
		}
	}

	BackHandler(
		enabled = isAnySelected,
		onBack = { onScreenEvent(TrashRecordingScreenEvent.OnUnSelectTrashRecording) },
	)

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
				isVisible = isAnySelected,
				onItemsDelete = { onScreenEvent(TrashRecordingScreenEvent.OnSelectItemDeleteForeEver) },
				onItemsRestore = { onScreenEvent(TrashRecordingScreenEvent.OnSelectItemRestore) },
			)
		},
		snackbarHost = { SnackbarHost(hostState = snackBarProvider) },
		modifier = modifier
			.nestedScroll(scrollBehavior.nestedScrollConnection)
			.sharedBoundsWrapper(key = SharedElementTransitionKeys.RECORDING_BIN_SHARED_BOUNDS),
	) { scPadding ->
		MediaAccessPermissionWrapper(
			onLoadRecordings = { onScreenEvent(TrashRecordingScreenEvent.PopulateTrashRecordings) },
			modifier = Modifier.padding(scPadding)
		) {
			TrashRecordingsInteractiveList(
				isRecordingsLoaded = isRecordingsLoaded,
				recordings = recordings,
				onItemSelect = { record ->
					onScreenEvent(TrashRecordingScreenEvent.OnRecordingSelectOrUnSelect(record))
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

private class SelectableTrashRecordingPreviewParams :
	CollectionPreviewParameterProvider<ImmutableList<SelectableTrashRecordings>>(
		listOf(
			RecordingsPreviewFakes.FAKE_TRASH_RECORDINGS_EMPTY,
			RecordingsPreviewFakes.FAKE_TRASH_RECORDINGS_MODELS
		)
	)

@PreviewLightDark
@Composable
private fun RecordingsBinScreenPreview(
	@PreviewParameter(SelectableTrashRecordingPreviewParams::class)
	recordings: ImmutableList<SelectableTrashRecordings>,
) = RecorderAppTheme {
	RecordingsBinScreen(
		isRecordingsLoaded = true,
		recordings = recordings,
		onScreenEvent = {},
		navigation = {
			Icon(
				imageVector = Icons.AutoMirrored.Default.ArrowBack,
				contentDescription = ""
			)
		},
	)
}