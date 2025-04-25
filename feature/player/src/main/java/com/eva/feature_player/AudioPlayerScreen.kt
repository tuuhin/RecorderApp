package com.eva.feature_player

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
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
import com.eva.bookmarks.domain.AudioBookmarkModel
import com.eva.feature_player.bookmarks.state.BookMarkEvents
import com.eva.feature_player.bookmarks.state.CreateBookmarkState
import com.eva.feature_player.bookmarks.utils.BookmarksPreviewFakes
import com.eva.feature_player.composable.AudioFileNotFoundBox
import com.eva.feature_player.composable.AudioPlayerScreenTopBar
import com.eva.feature_player.composable.FileMetadataDetailsSheet
import com.eva.feature_player.composable.PlayerActionsAndSlider
import com.eva.feature_player.composable.PlayerAmplitudeGraph
import com.eva.feature_player.composable.PlayerBookMarks
import com.eva.feature_player.state.AudioPlayerState
import com.eva.feature_player.state.PlayerEvents
import com.eva.player_shared.UserAudioAction
import com.eva.player_shared.composables.ContentLoadStatePreviewParams
import com.eva.player_shared.composables.ContentStateAnimatedContainer
import com.eva.player_shared.composables.PlayerDurationText
import com.eva.player_shared.util.AudioFileModelLoadState
import com.eva.player_shared.util.PlayerGraphData
import com.eva.player_shared.util.PlayerPreviewFakes
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.ui.R
import com.eva.ui.animation.SharedElementTransitionKeys
import com.eva.ui.animation.sharedBoundsWrapper
import com.eva.ui.theme.RecorderAppTheme
import com.eva.ui.utils.LocalSnackBarProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

@OptIn(
	ExperimentalSharedTransitionApi::class,
	ExperimentalMaterial3Api::class
)
@Composable
internal fun AudioPlayerScreenContainer(
	audioId: Long,
	loadState: AudioFileModelLoadState,
	content: @Composable BoxScope.(AudioFileModel) -> Unit,
	modifier: Modifier = Modifier,
	onFileEvent: (UserAudioAction) -> Unit = {},
	navigation: @Composable () -> Unit = {},
	onNavigateToEdit: () -> Unit = {},
	onRenameItem: (Long) -> Unit = {},
) {

	val snackBarProvider = LocalSnackBarProvider.current
	val scope = rememberCoroutineScope()

	val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
	var showBottomSheet by remember { mutableStateOf(false) }

	FileMetadataDetailsSheet(
		contentLoadState = loadState,
		sheetState = sheetState,
		showBottomSheet = showBottomSheet,
		onSheetDismiss = { showBottomSheet = false },
	)

	Scaffold(
		topBar = {
			AudioPlayerScreenTopBar(
				loadState = loadState,
				navigation = navigation,
				onEdit = onNavigateToEdit,
				onDetailsOptions = {
					scope.launch { sheetState.show() }
						.invokeOnCompletion { showBottomSheet = true }
				},
				onShareOption = { onFileEvent(UserAudioAction.ShareCurrentAudioFile) },
				onRenameOption = { model -> onRenameItem(model.id) },
				onToggleFavourite = { model ->
					onFileEvent(UserAudioAction.ToggleIsFavourite(model))
				}
			)
		},
		snackbarHost = { SnackbarHost(hostState = snackBarProvider) },
		modifier = modifier.sharedBoundsWrapper(
			key = SharedElementTransitionKeys.recordSharedEntryContainer(audioId),
			resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
			enter = fadeIn(animationSpec = tween(easing = EaseOut, durationMillis = 300)),
			exit = fadeOut(animationSpec = tween(easing = EaseOut, durationMillis = 300)),
		),
	) { scPadding ->
		ContentStateAnimatedContainer(
			loadState = loadState,
			modifier = Modifier
				.padding(scPadding)
				.padding(all = dimensionResource(id = R.dimen.sc_padding))
				.fillMaxSize(),
			onSuccess = content,
			onFailed = {
				AudioFileNotFoundBox(modifier = Modifier.align(Alignment.Center))
			},
			onLoading = {
				Column(
					verticalArrangement = Arrangement.spacedBy(8.dp),
					horizontalAlignment = Alignment.CenterHorizontally,
					modifier = Modifier.align(Alignment.Center)
				) {
					CircularProgressIndicator()
					Text(
						text = "Preparing player",
						style = MaterialTheme.typography.titleMedium,
						color = MaterialTheme.colorScheme.onSurface
					)
				}
			},
		)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AudioPlayerScreenContent(
	fileModel: AudioFileModel,
	waveforms: PlayerGraphData,
	bookMarkState: CreateBookmarkState,
	playerState: AudioPlayerState,
	bookmarks: ImmutableList<AudioBookmarkModel>,
	onPlayerEvents: (PlayerEvents) -> Unit,
	modifier: Modifier = Modifier,
	onBookmarkEvent: (BookMarkEvents) -> Unit = {},
) {
	val bookMarkTimeStamps by remember(bookmarks) {
		derivedStateOf {
			bookmarks.map(AudioBookmarkModel::timeStamp).toImmutableList()
		}
	}

	Box(
		modifier = modifier.fillMaxSize()
	) {
		PlayerDurationText(
			track = playerState.trackData,
			fileModel = fileModel,
			modifier = Modifier.align(Alignment.TopCenter),
		)
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.align(Alignment.Center)
				.offset(y = (-80).dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(4.dp),
		) {
			PlayerAmplitudeGraph(
				trackData = playerState.trackData,
				bookMarksTimeStamps = bookMarkTimeStamps,
				graphData = waveforms,
				modifier = Modifier.fillMaxWidth()
			)
			PlayerBookMarks(
				trackData = playerState.trackData,
				bookmarks = bookmarks,
				bookMarkState = bookMarkState,
				onBookmarkEvent = onBookmarkEvent,
				modifier = Modifier.fillMaxWidth()
			)
		}
		PlayerActionsAndSlider(
			metaData = playerState.playerMetaData,
			trackData = playerState.trackData,
			isControllerSet = playerState.isControllerSet,
			onPlayerAction = onPlayerEvents,
			modifier = Modifier
				.fillMaxWidth()
				.align(Alignment.BottomCenter),
		)
	}
}


@PreviewLightDark
@Composable
private fun AudioPlayerScreenPreview(
	@PreviewParameter(ContentLoadStatePreviewParams::class)
	loadState: AudioFileModelLoadState
) = RecorderAppTheme {
	AudioPlayerScreenContainer(
		audioId = 0,
		loadState = loadState,
		content = { model ->
			AudioPlayerScreenContent(
				fileModel = model,
				waveforms = { PlayerPreviewFakes.PREVIEW_RECORDER_AMPLITUDES },
				playerState = AudioPlayerState(isControllerSet = true),
				bookMarkState = CreateBookmarkState(),
				bookmarks = BookmarksPreviewFakes.FAKE_BOOKMARKS_LIST,
				onPlayerEvents = {},
			)
		},
	)
}