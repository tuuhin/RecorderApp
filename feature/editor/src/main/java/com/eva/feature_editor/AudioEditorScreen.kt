package com.eva.feature_editor

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.editor.data.AudioClipConfig
import com.eva.editor.domain.TransformationProgress
import com.eva.feature_editor.composables.EditorActionsAndControls
import com.eva.feature_editor.composables.EditorTopBar
import com.eva.feature_editor.composables.PlayerTrimSelector
import com.eva.feature_editor.composables.TransformationChip
import com.eva.feature_editor.event.EditorScreenEvent
import com.eva.player.domain.model.PlayerTrackData
import com.eva.player_shared.composables.ContentStateAnimatedContainer
import com.eva.player_shared.composables.PlayerDurationText
import com.eva.player_shared.state.ContentLoadState
import com.eva.player_shared.util.PlayerGraphData
import com.eva.player_shared.util.PlayerPreviewFakes
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.ui.R
import com.eva.ui.animation.SharedElementTransitionKeys
import com.eva.ui.animation.sharedBoundsWrapper
import com.eva.ui.theme.RecorderAppTheme
import com.eva.ui.utils.LocalSnackBarProvider
import kotlin.time.Duration.Companion.seconds

@OptIn(
	ExperimentalMaterial3Api::class,
	ExperimentalSharedTransitionApi::class
)
@Composable
internal fun AudioEditorScreenContainer(
	loadState: ContentLoadState<AudioFileModel>,
	content: @Composable BoxScope.(AudioFileModel) -> Unit,
	modifier: Modifier = Modifier,
	navigation: @Composable () -> Unit = {},
) {

	val snackBarHostProvider = LocalSnackBarProvider.current
	val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

	Scaffold(
		topBar = {
			AnimatedVisibility(
				visible = loadState is ContentLoadState.Content
			) {
				EditorTopBar(
					onSave = {},
					scrollBehavior = scrollBehavior,
					navigation = navigation
				)
			}
		},
		snackbarHost = { SnackbarHost(snackBarHostProvider) },
		modifier = modifier
			.nestedScroll(scrollBehavior.nestedScrollConnection)
			.sharedBoundsWrapper(
				key = SharedElementTransitionKeys.RECORDING_EDITOR_SHARED_BOUNDS,
				resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
				enter = fadeIn(animationSpec = tween(easing = EaseOut, durationMillis = 300)),
				exit = fadeOut(animationSpec = tween(easing = EaseOut, durationMillis = 300)),
			)
	) { scPadding ->
		ContentStateAnimatedContainer(
			loadState = loadState,
			modifier = Modifier
				.padding(scPadding)
				.padding(all = dimensionResource(id = R.dimen.sc_padding))
				.fillMaxSize(),
			onSuccess = content,
			onFailed = {},
		)
	}
}

@Composable
internal fun AudioEditorScreenContent(
	fileModel: AudioFileModel,
	trackData: PlayerTrackData,
	graphData: PlayerGraphData,
	onEvent: (EditorScreenEvent) -> Unit,
	modifier: Modifier = Modifier,
	isPlaying: Boolean = false,
	clipConfig: AudioClipConfig? = null,
	transformation: TransformationProgress = TransformationProgress.Idle,
) {
	Box(
		modifier = modifier
			.fillMaxSize()
			.windowInsetsPadding(WindowInsets.safeGestures),
	) {
		PlayerDurationText(
			track = trackData,
			fileModel = fileModel,
			modifier = Modifier.align(Alignment.TopCenter)
		)
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.align(Alignment.Center)
				.offset(y = (-80).dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(4.dp),
		) {
			PlayerTrimSelector(
				graphData = graphData,
				trackData = trackData,
				clipConfig = clipConfig,
				onClipConfigChange = { onEvent(EditorScreenEvent.OnClipConfigChange(it)) },
				modifier = Modifier.fillMaxWidth()
			)
			TransformationChip(
				progress = transformation,
				modifier = Modifier.align(Alignment.CenterHorizontally)
			)
		}
		Box(
			modifier = Modifier
				.heightIn(min = 180.dp)
				.fillMaxWidth()
				.align(Alignment.BottomCenter),
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

@PreviewLightDark
@Composable
private fun AudioEditorScreenPreview() = RecorderAppTheme {
	AudioEditorScreenContainer(
		loadState = ContentLoadState.Content(PlayerPreviewFakes.FAKE_AUDIO_MODEL),
		content = { model ->
			AudioEditorScreenContent(
				fileModel = model,
				trackData = PlayerTrackData(total = 10.seconds),
				graphData = { PlayerPreviewFakes.PREVIEW_RECORDER_AMPLITUDES },
				onEvent = {},
			)
		},
		navigation = {
			Icon(
				imageVector = Icons.AutoMirrored.Default.ArrowBack,
				contentDescription = ""
			)
		},
	)
}