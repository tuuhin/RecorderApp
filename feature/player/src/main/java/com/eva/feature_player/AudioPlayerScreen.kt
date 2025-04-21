package com.eva.feature_player

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.unit.dp
import com.eva.bookmarks.domain.AudioBookmarkModel
import com.eva.feature_player.bookmarks.state.BookMarkEvents
import com.eva.feature_player.bookmarks.state.CreateBookmarkState
import com.eva.feature_player.bookmarks.utils.BookmarksPreviewFakes
import com.eva.feature_player.composable.AudioFileNotFoundBox
import com.eva.feature_player.composable.AudioPlayerScreenTopBar
import com.eva.feature_player.composable.FileMetaDataSheetContent
import com.eva.feature_player.composable.PlayerActionsAndSlider
import com.eva.feature_player.composable.PlayerAmplitudeGraph
import com.eva.feature_player.composable.PlayerBookMarks
import com.eva.feature_player.state.AudioPlayerState
import com.eva.feature_player.state.PlayerEvents
import com.eva.player_shared.UserAudioAction
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

@OptIn(
	ExperimentalMaterial3Api::class,
	ExperimentalSharedTransitionApi::class
)
@Composable
internal fun AudioPlayerScreen(
	selectedAudioId: Long,
	waveforms: PlayerGraphData,
	bookMarkState: CreateBookmarkState,
	playerState: AudioPlayerState,
	bookmarks: ImmutableList<AudioBookmarkModel>,
	loadState: ContentLoadState<out AudioFileModel>,
	onPlayerEvents: (PlayerEvents) -> Unit,
	modifier: Modifier = Modifier,
	onFileEvent: (UserAudioAction) -> Unit = {},
	onBookmarkEvent: (BookMarkEvents) -> Unit = {},
	navigation: @Composable () -> Unit = {},
	onNavigateToEdit: () -> Unit = {},
	onRenameItem: (Long) -> Unit = {},
) {
	val snackBarProvider = LocalSnackBarProvider.current
	val scope = rememberCoroutineScope()

	val metaDataBottomSheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

	var openMetaDataBottomSheet by remember { mutableStateOf(false) }

	val canShowMetaDataBottomSheet by remember(loadState, openMetaDataBottomSheet) {
		derivedStateOf {
			loadState is ContentLoadState.Content && openMetaDataBottomSheet
		}
	}

	val bookMarkTimeStamps by remember(bookmarks) {
		derivedStateOf {
			bookmarks.map(AudioBookmarkModel::timeStamp).toImmutableList()
		}
	}

	if (canShowMetaDataBottomSheet) {
		ModalBottomSheet(
			sheetState = metaDataBottomSheet,
			onDismissRequest = { openMetaDataBottomSheet = false },
		) {
			loadState.OnContent { audio ->
				FileMetaDataSheetContent(
					audio = audio,
					contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.bottom_sheet_padding_lg))
				)
			}
		}
	}

	Scaffold(
		topBar = {
			AudioPlayerScreenTopBar(
				state = loadState,
				navigation = navigation,
				onEdit = onNavigateToEdit,
				onDetailsOptions = {
					scope.launch { metaDataBottomSheet.show() }
						.invokeOnCompletion { openMetaDataBottomSheet = true }
				},
				onShareOption = { onFileEvent(UserAudioAction.ShareCurrentAudioFile) },
				onRenameOption = { model -> onRenameItem(model.id) },
				onToggleFavourite = { model -> onFileEvent(UserAudioAction.ToggleIsFavourite(model)) }
			)
		},
		snackbarHost = { SnackbarHost(hostState = snackBarProvider) },
		modifier = modifier.sharedBoundsWrapper(
			key = SharedElementTransitionKeys.recordSharedEntryContainer(selectedAudioId),
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
			onSuccess = {
				PlayerDurationText(
					track = playerState.trackData,
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
			},
			onFailed = {
				AudioFileNotFoundBox(modifier = Modifier.align(Alignment.Center))
			},
		)
	}
}

@PreviewLightDark
@Composable
private fun AudioPlayerScreenPreview() = RecorderAppTheme {
	AudioPlayerScreen(
		selectedAudioId = 0L,
		waveforms = { PlayerPreviewFakes.PREVIEW_RECORDER_AMPLITUDES },
		playerState = AudioPlayerState(isControllerSet = true),
		bookmarks = BookmarksPreviewFakes.FAKE_BOOKMARKS_LIST,
		bookMarkState = CreateBookmarkState(),
		loadState = ContentLoadState.Content(data = PlayerPreviewFakes.FAKE_AUDIO_MODEL),
		onPlayerEvents = {},
		navigation = {
			Icon(
				imageVector = Icons.AutoMirrored.Default.ArrowBack,
				contentDescription = ""
			)
		},
	)
}