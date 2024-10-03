package com.eva.recorderapp.voice_recorder.presentation.record_player

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.presentation.record_player.composable.AudioFileMetaDataSheetContent
import com.eva.recorderapp.voice_recorder.presentation.record_player.composable.AudioPlayerScreenTopBar
import com.eva.recorderapp.voice_recorder.presentation.record_player.composable.ContentStateAnimatedContainer
import com.eva.recorderapp.voice_recorder.presentation.record_player.composable.PlayerActionsAndSlider
import com.eva.recorderapp.voice_recorder.presentation.record_player.composable.PlayerAmplitudeGraph
import com.eva.recorderapp.voice_recorder.presentation.record_player.composable.PlayerBookMarks
import com.eva.recorderapp.voice_recorder.presentation.record_player.composable.PlayerDurationText
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.AudioFileEvent
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.AudioPlayerInformation
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.BookMarkEvents
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.ContentLoadState
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.CreateOrEditBookMarkState
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.PlayerEvents
import com.eva.recorderapp.voice_recorder.presentation.util.LocalSnackBarProvider
import com.eva.recorderapp.voice_recorder.presentation.util.PlayerGraphData
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
	waveforms: PlayerGraphData,
	bookMarkState: CreateOrEditBookMarkState,
	playerState: AudioPlayerInformation,
	loadState: ContentLoadState,
	onPlayerEvents: (PlayerEvents) -> Unit,
	modifier: Modifier = Modifier,
	onFileEvent: (AudioFileEvent) -> Unit = {},
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

	if (canShowMetaDataBottomSheet) {
		ModalBottomSheet(
			sheetState = metaDataBottomSheet,
			onDismissRequest = { openMetaDataBottomSheet = false },
		) {
			loadState.OnContent { audio ->
				AudioFileMetaDataSheetContent(
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
				onShareOption = { onFileEvent(AudioFileEvent.ShareCurrentAudioFile) },
				onRenameOption = { model -> onRenameItem(model.id) },
				onToggleFavourite = { model -> onFileEvent(AudioFileEvent.ToggleIsFavourite(model)) }
			)
		},
		snackbarHost = { SnackbarHost(hostState = snackBarProvider) },
		modifier = modifier,
	) { scPadding ->
		ContentStateAnimatedContainer(
			loadState = loadState,
			modifier = Modifier
				.padding(
					start = dimensionResource(id = R.dimen.sc_padding),
					end = dimensionResource(R.dimen.sc_padding),
					top = dimensionResource(R.dimen.sc_padding) + scPadding.calculateTopPadding(),
					bottom = dimensionResource(R.dimen.sc_padding) + scPadding.calculateBottomPadding()
				)
				.fillMaxSize(),
			onSuccess = {
				PlayerDurationText(
					track = playerState.trackData,
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
					PlayerAmplitudeGraph(
						trackData = playerState.trackData,
						graphData = waveforms,
						bookMarks = playerState.bookmarksTimestamps,
						modifier = Modifier.fillMaxWidth()
					)
					PlayerBookMarks(
						trackData = playerState.trackData,
						bookmarks = playerState.bookmarks,
						bookMarkState = bookMarkState,
						onBookmarkEvent = onBookmarkEvent,
						modifier = Modifier.fillMaxWidth()
					)
				}
				PlayerActionsAndSlider(
					metaData = playerState.playerMetaData,
					trackData = playerState.trackData,
					onPlayerAction = onPlayerEvents,
					modifier = Modifier
						.fillMaxWidth()
						.align(Alignment.BottomCenter),
				)
			},
		)
	}
}

@PreviewLightDark
@Composable
private fun AudioPlayerScreenPreview() = RecorderAppTheme {
	AudioPlayerScreen(
		waveforms = { PreviewFakes.PREVIEW_RECORDER_AMPLITUDES },
		playerState = PreviewFakes.FAKE_AUDIO_INFORMATION,
		bookMarkState = CreateOrEditBookMarkState(),
		loadState = ContentLoadState.Content(data = PreviewFakes.FAKE_AUDIO_MODEL),
		onPlayerEvents = {},
		navigation = {
			Icon(
				imageVector = Icons.AutoMirrored.Default.ArrowBack,
				contentDescription = stringResource(R.string.back_arrow)
			)
		},
	)
}