package com.eva.feature_editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.feature_editor.composables.EditorActionsAndControls
import com.eva.feature_editor.composables.EditorTopBar
import com.eva.feature_editor.composables.PlayerTrimSelector
import com.eva.feature_editor.event.EditorScreenEvent
import com.eva.feature_editor.util.PlayerEditorPreviewFakes
import com.eva.player.domain.model.PlayerTrackData
import com.eva.player_shared.composables.ContentStateAnimatedContainer
import com.eva.player_shared.composables.PlayerDurationText
import com.eva.player_shared.state.ContentLoadState
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme
import com.eva.ui.utils.LocalSnackBarProvider
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AudioEditorScreen(
	loadState: ContentLoadState<out AudioFileModel>,
	track: PlayerTrackData,
	onEvent: (EditorScreenEvent) -> Unit,
	modifier: Modifier = Modifier,
	isPlaying: Boolean = false,
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
		modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
	) { scPadding ->
		ContentStateAnimatedContainer(
			loadState = loadState,
			modifier = Modifier
				.padding(scPadding)
				.padding(all = dimensionResource(id = R.dimen.sc_padding))
				.fillMaxSize(),
			onSuccess = {
				PlayerDurationText(
					track = track,
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
					// we will have an editor implementation
					PlayerTrimSelector(modifier = Modifier.fillMaxWidth())
					// other track helper
				}
				Box(
					modifier = Modifier
						.heightIn(min = 180.dp)
						.fillMaxWidth()
						.align(Alignment.BottomCenter),
					contentAlignment = Alignment.Center
				) {
					EditorActionsAndControls(
						trackData = track,
						isMediaPlaying = isPlaying,
						onEvent = onEvent,
						modifier = Modifier.fillMaxWidth()
					)
				}

			},
			onFailed = {},
		)
	}
}


@PreviewLightDark
@Composable
private fun AudioEditorScreenPreview() = RecorderAppTheme {
	AudioEditorScreen(
		loadState = ContentLoadState.Content(PlayerEditorPreviewFakes.FAKE_AUDIO_MODEL),
		track = PlayerTrackData(2.seconds, 20.seconds),
		onEvent = {},
		navigation = {
			Icon(
				imageVector = Icons.AutoMirrored.Default.ArrowBack,
				contentDescription = ""
			)
		},
	)
}