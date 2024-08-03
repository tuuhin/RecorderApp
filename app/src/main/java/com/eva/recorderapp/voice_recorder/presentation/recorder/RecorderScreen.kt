package com.eva.recorderapp.voice_recorder.presentation.recorder

import android.Manifest
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.emums.RecorderAction
import com.eva.recorderapp.voice_recorder.domain.emums.RecorderState
import com.eva.recorderapp.voice_recorder.presentation.recorder.composable.AnimatedRecorderActionTray
import com.eva.recorderapp.voice_recorder.presentation.recorder.composable.NoRecordPermissionBox
import com.eva.recorderapp.voice_recorder.presentation.recorder.composable.RecorderAmplitudeGraph
import com.eva.recorderapp.voice_recorder.presentation.recorder.composable.RecorderTimerText
import com.eva.recorderapp.voice_recorder.presentation.recorder.composable.RecorderTopBar
import com.eva.recorderapp.voice_recorder.presentation.util.LocalSnackBarProvider
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRecroderScreen(
	stopWatch: LocalTime,
	recorderState: RecorderState,
	recorderAmps: ImmutableList<Float>,
	onRecorderAction: (RecorderAction) -> Unit,
	onShowRecordings: () -> Unit,
	onNavigateToSettings: () -> Unit,
	onNavigateToBin: () -> Unit,
	modifier: Modifier = Modifier,
	navigation: @Composable () -> Unit = {},
) {

	val context = LocalContext.current
	val snackBarHostState = LocalSnackBarProvider.current

	var hasRecordPermission by remember {
		mutableStateOf(
			ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
					PermissionChecker.PERMISSION_GRANTED
		)
	}

	val canShowActions by remember(recorderState) {
		derivedStateOf {
			recorderState in arrayOf(
				RecorderState.IDLE,
				RecorderState.COMPLETED,
				RecorderState.CANCELLED
			)
		}
	}

	Scaffold(
		topBar = {
			RecorderTopBar(
				showActions = canShowActions,
				onShowRecordings = onShowRecordings,
				onNavigateToSettings = onNavigateToSettings,
				onNavigateToBin = onNavigateToBin,
				navigation = navigation
			)
		},
		snackbarHost = { SnackbarHost(snackBarHostState) },
		modifier = modifier,
	) { scPadding ->
		Crossfade(
			targetState = hasRecordPermission,
			modifier = Modifier
				.fillMaxSize()
				.padding(scPadding)
				.padding(horizontal = dimensionResource(id = R.dimen.sc_padding)),
			label = "Has record audio permission"
		) { hasPerms ->
			if (hasPerms) {
				Box(
					modifier = Modifier.fillMaxSize(),
					contentAlignment = Alignment.Center
				) {
					Column(
						horizontalAlignment = Alignment.CenterHorizontally,
						verticalArrangement = Arrangement.spacedBy(20.dp),
						modifier = Modifier.offset(y = dimensionResource(id = R.dimen.graph_offset))
					) {
						RecorderTimerText(time = stopWatch)
						RecorderAmplitudeGraph(
							amplitudes = recorderAmps,
							barColor = MaterialTheme.colorScheme.onSecondaryContainer,
							backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
							shape = MaterialTheme.shapes.medium,
							modifier = Modifier.fillMaxWidth(),
						)
					}
					AnimatedRecorderActionTray(
						recorderState = recorderState,
						onRecorderAction = onRecorderAction,
						modifier = Modifier
							.offset(y = dimensionResource(id = R.dimen.recordings_action_offset))
							.fillMaxWidth()
							.align(Alignment.BottomCenter)
					)
				}
			} else {
				Box(
					modifier = Modifier.fillMaxSize(),
					contentAlignment = Alignment.Center
				) {
					NoRecordPermissionBox(
						onPermsChanged = { perms -> hasRecordPermission = perms },
						modifier = Modifier.padding(12.dp)
					)
				}
			}
		}
	}
}

private class RecorderStatePreviewParams :
	CollectionPreviewParameterProvider<RecorderState>(
		listOf(
			RecorderState.RECORDING,
			RecorderState.COMPLETED,
			RecorderState.PAUSED
		)
	)

@PreviewLightDark
@Composable
private fun VoiceRecorderScreenPreview(
	@PreviewParameter(RecorderStatePreviewParams::class)
	recorderState: RecorderState
) = RecorderAppTheme {
	VoiceRecroderScreen(
		stopWatch = LocalTime(0, 10, 56, 0),
		recorderState = recorderState,
		recorderAmps = PreviewFakes.PREVIEW_RECORDER_AMPLITUDES,
		onRecorderAction = {},
		onShowRecordings = {},
		onNavigateToSettings = {},
		onNavigateToBin = {},
	)
}