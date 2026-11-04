package com.eva.feature_editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.eva.editor.domain.model.AudioClipConfig
import com.eva.feature_editor.composables.AudioClipChipRow
import com.eva.feature_editor.composables.EditorActionsAndControls
import com.eva.feature_editor.composables.EditorBackHandlerDialog
import com.eva.feature_editor.composables.EditorTopBar
import com.eva.feature_editor.composables.PlayerTrimSelector
import com.eva.feature_editor.composables.TransformBottomSheet
import com.eva.feature_editor.event.EditorScreenEvent
import com.eva.feature_editor.event.TransformationState
import com.eva.feature_editor.undoredo.UndoRedoState
import com.eva.player.domain.model.PlayerTrackData
import com.eva.player_shared.composables.AudioFileNotFoundBox
import com.eva.player_shared.composables.ContentLoadStatePreviewParams
import com.eva.player_shared.composables.ContentStateAnimatedContainer
import com.eva.player_shared.composables.PlayerDurationText
import com.eva.player_shared.state.ContentLoadState
import com.eva.player_shared.util.PlayerGraphData
import com.eva.player_shared.util.PlayerPreviewFakes
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.ui.R
import com.eva.ui.animation.SharedElementTransitionKeys
import com.eva.ui.animation.sharedBoundsWrapper
import com.eva.ui.animation.sharedTransitionSkipChildPosition
import com.eva.ui.animation.sharedTransitionSkipChildSize
import com.eva.ui.theme.DownloadableFonts
import com.eva.ui.theme.RecorderAppTheme
import com.eva.ui.utils.LocalSnackBarProvider
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AudioEditorScreen(
	loadState: ContentLoadState<AudioFileModel>,
	trackData: () -> PlayerTrackData,
	graphData: PlayerGraphData,
	onEvent: (EditorScreenEvent) -> Unit,
	modifier: Modifier = Modifier,
	isPlaying: Boolean = false,
	clipConfig: AudioClipConfig? = null,
	isMediaEdited: Boolean = false,
	isVisualsReady: Boolean = false,
	undoRedoState: UndoRedoState = UndoRedoState(),
	transformationState: TransformationState = TransformationState(),
	navigation: @Composable () -> Unit = {},
	onDismissScreen: () -> Unit = {},
) {
	val snackBarHostProvider = LocalSnackBarProvider.current
	val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

	val isBackHandlerEnabled by rememberSaveable(undoRedoState.holdHistory) {
		mutableStateOf(undoRedoState.holdHistory)
	}
	var showDialog by remember { mutableStateOf(false) }

	var showSheet by remember { mutableStateOf(false) }
	val bottomSheetState = rememberModalBottomSheetState()
	val scope = rememberCoroutineScope()

	val totalTrackDuration by remember { derivedStateOf { trackData().total } }

	TransformBottomSheet(
		onDismiss = {
			scope.launch { bottomSheetState.hide() }
				.invokeOnCompletion { showSheet = false }
		},
		state = transformationState,
		onEvent = onEvent,
		bottomSheetState = bottomSheetState,
		showSheet = showSheet
	)

	BackHandler(enabled = isBackHandlerEnabled, onBack = { showDialog = true })

	EditorBackHandlerDialog(
		showDialog = showDialog,
		onConfirm = onDismissScreen,
		onDismiss = { showDialog = false },
		properties = DialogProperties(dismissOnClickOutside = false)
	)

	Scaffold(
		topBar = {
			EditorTopBar(
                onExport = {
                    scope.launch { bottomSheetState.show() }
                        .invokeOnCompletion { showSheet = true }
                },
                scrollBehavior = scrollBehavior,
                isActionsEnabled = isMediaEdited && isVisualsReady,
                isEditingEnabled = isVisualsReady,
                state = undoRedoState,
                onRedoAction = { onEvent(EditorScreenEvent.OnRedoEdit) },
                onUndoAction = { onEvent(EditorScreenEvent.OnUndoEdit) },
                navigation = navigation,
                modifier = Modifier.sharedTransitionSkipChildSize(),
            )
		},
		snackbarHost = { SnackbarHost(snackBarHostProvider) },
		modifier = modifier.sharedBoundsWrapper(
			key = SharedElementTransitionKeys.RECORDING_EDITOR_SHARED_BOUNDS,
			resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
			clipShape = MaterialTheme.shapes.large
		),
	) { scPadding ->
		ContentStateAnimatedContainer(
            loadState = loadState,
            onSuccess = { model ->
                PlayerDurationText(
                    track = {
                        val track = trackData()
                        if (track.allPositiveAndFinite) track
                        else PlayerTrackData(Duration.ZERO, model.duration)
                    },
                    fontFamily = DownloadableFonts.SPLINE_SANS_MONO_FONT_FAMILY,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .offset(y = (-80).dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PlayerTrimSelector(
                        graphData = graphData,
                        trackData = {
                            val track = trackData()
                            if (track.allPositiveAndFinite) track
                            else PlayerTrackData(Duration.ZERO, model.duration)
                        },
                        enabled = isVisualsReady,
                        clipConfig = clipConfig,
                        onClipConfigChange = { onEvent(EditorScreenEvent.OnClipConfigChange(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(
                            horizontal = dimensionResource(R.dimen.graph_card_padding),
                            vertical = dimensionResource(R.dimen.graph_card_padding_other)
                        )
                    )
                    AudioClipChipRow(
                        clipConfig = clipConfig,
                        onEvent = onEvent,
                        trackDuration = totalTrackDuration,
                        enabled = isVisualsReady,
                    )
                }
                Box(
                    modifier = Modifier
                        .heightIn(min = 180.dp)
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .offset(y = (-20).dp),
                    contentAlignment = Alignment.Center
                ) {
                    EditorActionsAndControls(
                        trackData = trackData,
                        isMediaPlaying = isPlaying,
                        onEvent = onEvent,
                        isEditingEnabled = isVisualsReady,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            onFailed = {
                AudioFileNotFoundBox(
                    onNavigateToList = {},
                    modifier = Modifier.align(Alignment.Center)
                )
            },
            modifier = Modifier
                .padding(scPadding)
                .padding(all = dimensionResource(id = R.dimen.sc_padding))
                .sharedTransitionSkipChildSize()
                .sharedTransitionSkipChildPosition(),
        )
	}
}

@PreviewLightDark
@Composable
private fun AudioEditorScreenPreview(
	@PreviewParameter(ContentLoadStatePreviewParams::class)
	loadState: ContentLoadState<AudioFileModel>,
) = RecorderAppTheme {
	AudioEditorScreen(
		loadState = loadState,
		trackData = { PlayerTrackData(total = 10.seconds) },
		graphData = { PlayerPreviewFakes.loadAmplitudeGraph(10.seconds) },
		clipConfig = AudioClipConfig(end = 10.seconds),
		onEvent = {},
		navigation = {
			Icon(
				imageVector = Icons.AutoMirrored.Default.ArrowBack,
				contentDescription = ""
			)
		},
	)
}
