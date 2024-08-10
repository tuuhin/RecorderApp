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
import com.eva.recorderapp.voice_recorder.presentation.record_player.composable.AudioPlayerActions
import com.eva.recorderapp.voice_recorder.presentation.record_player.composable.AudioPlayerScreenTopBar
import com.eva.recorderapp.voice_recorder.presentation.record_player.composable.ContentStateLoading
import com.eva.recorderapp.voice_recorder.presentation.record_player.composable.PlayBackSpeedSelector
import com.eva.recorderapp.voice_recorder.presentation.record_player.composable.PlayerDurationText
import com.eva.recorderapp.voice_recorder.presentation.record_player.composable.PlayerGraphAndBookMarks
import com.eva.recorderapp.voice_recorder.presentation.record_player.composable.PlayerSlider
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.AudioPlayerInformation
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.ContentLoadState
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.PlayerEvents
import com.eva.recorderapp.voice_recorder.presentation.util.LocalSnackBarProvider
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
	playerState: AudioPlayerInformation,
	loadState: ContentLoadState,
	onPlayerEvents: (PlayerEvents) -> Unit,
	modifier: Modifier = Modifier,
	navigation: @Composable () -> Unit = {},
	onNavigateToEdit: () -> Unit = {},
) {
	val snackBarProvider = LocalSnackBarProvider.current
	val scope = rememberCoroutineScope()

	val metaDataBottomSheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
	val playBackSpeedBottomSheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

	var openMetaDataBottomSheet by remember { mutableStateOf(false) }
	var openPlayBackSpeedBottomSheet by remember { mutableStateOf(false) }

	val canShowMetaDataBottomSheet by remember(loadState, openMetaDataBottomSheet) {
		derivedStateOf {
			loadState is ContentLoadState.Content && openMetaDataBottomSheet
		}
	}

	if (openPlayBackSpeedBottomSheet) {
		ModalBottomSheet(
			sheetState = playBackSpeedBottomSheet,
			onDismissRequest = { openPlayBackSpeedBottomSheet = false },
		) {
			PlayBackSpeedSelector(
				selectedSpeed = playerState.playerMetaData.playBackSpeed,
				onSpeedSelected = { speed ->
					onPlayerEvents(PlayerEvents.OnPlayerSpeedChange(speed))
				},
				contentPadding = PaddingValues(dimensionResource(id = R.dimen.bottomsheet_padding_lg))
			)
		}
	}

	if (canShowMetaDataBottomSheet) {
		ModalBottomSheet(
			sheetState = metaDataBottomSheet,
			onDismissRequest = { openMetaDataBottomSheet = false },
		) {
			loadState.OnContent { audioFile ->
				AudioFileMetaDataSheetContent(audio = audioFile)
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
				onShareOption = { onPlayerEvents(PlayerEvents.ShareCurrentAudioFile) },
			)
		},
		snackbarHost = { SnackbarHost(hostState = snackBarProvider) },
		modifier = modifier,
	) { scPadding ->
		ContentStateLoading(
			loadState = loadState,
			modifier = Modifier
				.padding(scPadding)
				.padding(all = dimensionResource(id = R.dimen.sc_padding))
				.fillMaxSize(),
			onSuccess = { _ ->
				PlayerDurationText(
					track = playerState.trackData,
					modifier = Modifier.align(Alignment.TopCenter)
				)
				PlayerGraphAndBookMarks(
					samples = playerState.sampling,
					isGraphMode = true,
					onToggleListAndWave = { },
					onAddBookMark = { },
					modifier = Modifier
						.fillMaxWidth()
						.align(Alignment.Center)
						.offset(y = -80.dp)
				)
				Column(
					modifier = Modifier
						.fillMaxWidth()
						.align(Alignment.BottomCenter),
					verticalArrangement = Arrangement.spacedBy(8.dp)
				) {
					PlayerSlider(
						track = playerState.trackData,
						onSeekToDuration = { amount ->
							onPlayerEvents(PlayerEvents.OnSeekPlayer(amount))
						},
						onSeekDurationComplete = { onPlayerEvents(PlayerEvents.OnSeekComplete) },
					)
					AudioPlayerActions(
						playerMetaData = playerState.playerMetaData,
						onPlay = { onPlayerEvents(PlayerEvents.OnStartPlayer) },
						onPause = { onPlayerEvents(PlayerEvents.OnPausePlayer) },
						onMutePlayer = { onPlayerEvents(PlayerEvents.OnMutePlayer) },
						onRepeatModeChange = { onPlayerEvents(PlayerEvents.OnRepeatModeChange(it)) },
						onRewind = { onPlayerEvents(PlayerEvents.OnRewindByNDuration()) },
						onForward = { onPlayerEvents(PlayerEvents.OnForwardByNDuration()) },
						onSpeedChange = {
							scope.launch { playBackSpeedBottomSheet.show() }
								.invokeOnCompletion { openPlayBackSpeedBottomSheet = true }
						},
					)
				}
			},
		)
	}
}

@PreviewLightDark
@Composable
private fun AudioPlayerScreenPreview() = RecorderAppTheme {
	AudioPlayerScreen(
		playerState = PreviewFakes.FAKE_AUDIO_INFORMATION,
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