package com.eva.feature_editor

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.eva.editor.domain.model.AudioClipConfig
import com.eva.feature_editor.composables.AudioClipChipRow
import com.eva.feature_editor.composables.EditorActionsAndControls
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
import com.eva.ui.theme.DownloadableFonts
import com.eva.ui.theme.RecorderAppTheme
import com.eva.ui.utils.LocalSnackBarProvider
import kotlinx.coroutines.launch

@OptIn(
	ExperimentalMaterial3Api::class,
	ExperimentalSharedTransitionApi::class
)
@Composable
internal fun AudioEditorScreenContainer(
	loadState: ContentLoadState<AudioFileModel>,
	content: @Composable BoxScope.(AudioFileModel) -> Unit,
	modifier: Modifier = Modifier,
) {
	Surface(
		modifier = modifier
			.fillMaxSize()
			.sharedBoundsWrapper(
				key = SharedElementTransitionKeys.RECORDING_EDITOR_SHARED_BOUNDS,
				resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
				enter = fadeIn(animationSpec = tween(easing = EaseOut, durationMillis = 300)),
				exit = fadeOut(animationSpec = tween(easing = EaseOut, durationMillis = 300)),
			)
	) {
		ContentStateAnimatedContainer(
			loadState = loadState,
			onSuccess = content,
			onFailed = {
				AudioFileNotFoundBox(modifier = Modifier.align(Alignment.Center))
			},
		)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AudioEditorScreen(
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
) {
	val snackBarHostProvider = LocalSnackBarProvider.current
	val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

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

	Scaffold(
		topBar = {
			EditorTopBar(
				onExport = {
					scope.launch { bottomSheetState.show() }
						.invokeOnCompletion { showSheet = true }
				},
				scrollBehavior = scrollBehavior,
				isActionsEnabled = isMediaEdited,
				state = undoRedoState,
				onRedoAction = { onEvent(EditorScreenEvent.OnRedoEdit) },
				onUndoAction = { onEvent(EditorScreenEvent.OnUndoEdit) },
				navigation = navigation,
			)
		},
		snackbarHost = { SnackbarHost(snackBarHostProvider) },
		modifier = modifier,
	) { scPadding ->
		Box(
			modifier = Modifier
				.padding(scPadding)
				.padding(all = dimensionResource(id = R.dimen.sc_padding))
				.fillMaxSize(),
		) {
			PlayerDurationText(
				track = trackData,
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
					trackData = trackData,
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
					trackDuration = totalTrackDuration
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
					modifier = Modifier.fillMaxWidth()
				)
			}
		}
	}
}

@PreviewLightDark
@Composable
private fun AudioEditorScreenPreview(
	@PreviewParameter(ContentLoadStatePreviewParams::class)
	loadState: ContentLoadState<AudioFileModel>,
) = RecorderAppTheme {
	AudioEditorScreenContainer(
		loadState = loadState,
		content = { model ->
			AudioEditorScreen(
				trackData = { PlayerTrackData(total = model.duration) },
				graphData = { PlayerPreviewFakes.loadAmplitudeGraph(model.duration) },
				clipConfig = AudioClipConfig(end = model.duration),
				onEvent = {},
				navigation = {
					Icon(
						imageVector = Icons.AutoMirrored.Default.ArrowBack,
						contentDescription = ""
					)
				},
			)
		},
	)
}