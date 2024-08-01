package com.eva.recorderapp.voice_recorder.presentation.record_player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
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
import com.eva.recorderapp.voice_recorder.presentation.record_player.composable.PlayerDurationText
import com.eva.recorderapp.voice_recorder.presentation.record_player.composable.PlayerGraphAndBookMarks
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.ContentLoadState
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.PlayerEvents
import com.eva.recorderapp.voice_recorder.presentation.util.LocalSnackBarProvider
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
	loadState: ContentLoadState,
	onPlayerEvents: (PlayerEvents) -> Unit,
	modifier: Modifier = Modifier,
	navigation: @Composable () -> Unit = {},
	onNavigateToEdit: () -> Unit = {},
) {
	val snackBarProvider = LocalSnackBarProvider.current
	val scope = rememberCoroutineScope()
	val fileDetailsBottomSheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

	var openAudioDetailsSheet by remember { mutableStateOf(false) }

	val canShowFileDetailsSheet by remember(loadState, openAudioDetailsSheet) {
		derivedStateOf {
			loadState is ContentLoadState.Content && openAudioDetailsSheet
		}
	}

	if (canShowFileDetailsSheet) {
		ModalBottomSheet(
			onDismissRequest = { openAudioDetailsSheet = false },
			sheetState = fileDetailsBottomSheet,
			containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
		) {
			loadState.OnContent { audioFIle ->
				AudioFileMetaDataSheetContent(audio = audioFIle)
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
					scope.launch { fileDetailsBottomSheet.show() }
						.invokeOnCompletion { openAudioDetailsSheet = true }
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
			onSuccess = { audioModel ->
				PlayerDurationText(
					playedDuration = audioModel.durationAsLocaltime,
					totalDuration = audioModel.durationAsLocaltime,
					modifier = Modifier.align(Alignment.TopCenter)
				)
				PlayerGraphAndBookMarks(
					isGraphMode = true,
					onToggleListAndWave = { },
					onAddBookMark = { },
					modifier = Modifier
						.align(Alignment.Center)
						.offset(y = -30.dp)
				)
				AudioPlayerActions(
					isPlaying = true,
					onPlay = {},
					onPause = {},
					modifier = Modifier
						.align(Alignment.BottomCenter)
				)
			},
		)
	}
}

@PreviewLightDark
@Composable
private fun AudioPlayerScreenPreview() = RecorderAppTheme {
	AudioPlayerScreen(
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