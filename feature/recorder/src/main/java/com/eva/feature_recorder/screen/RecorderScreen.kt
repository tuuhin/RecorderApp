package com.eva.feature_recorder.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.eva.feature_recorder.composable.MicPermissionWrapper
import com.eva.feature_recorder.composable.RecorderContent
import com.eva.feature_recorder.composable.RecorderTopBar
import com.eva.feature_recorder.util.showTopBarActions
import com.eva.recorder.domain.models.RecorderAction
import com.eva.recorder.domain.models.RecorderState
import com.eva.recorder.utils.DeferredDurationList
import com.eva.recorder.utils.DeferredRecordingDataPointList
import com.eva.ui.R
import com.eva.ui.utils.LocalSnackBarProvider
import kotlinx.datetime.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VoiceRecorderScreen(
	isRecorderReady: Boolean,
	recorderState: RecorderState,
	recorderTimer: LocalTime,
	deferredRecordingPoints: DeferredRecordingDataPointList,
	bookMarksSetDeferred: DeferredDurationList,
	onRecorderAction: (RecorderAction) -> Unit,
	onScreenEvent: (RecorderScreenEvent) -> Unit,
	onShowRecordings: () -> Unit,
	onNavigateToSettings: () -> Unit,
	onNavigateToBin: () -> Unit,
	modifier: Modifier = Modifier,
	navigation: @Composable () -> Unit = {},
) {

	val snackBarHostState = LocalSnackBarProvider.current
	val lifecycleOwner = LocalLifecycleOwner.current

	val currentOnScreenEvent by rememberUpdatedState(onScreenEvent)

	LifecycleStartEffect(key1 = lifecycleOwner) {
		// bind the service on start
		currentOnScreenEvent(RecorderScreenEvent.BindRecorderService)
		onStopOrDispose {
			// unbind the service on stop
			currentOnScreenEvent(RecorderScreenEvent.UnBindRecorderService)
		}
	}

	Scaffold(
		topBar = {
			RecorderTopBar(
				showActions = recorderState.showTopBarActions,
				onNavigateToRecordings = onShowRecordings,
				onNavigateToSettings = onNavigateToSettings,
				onNavigateToBin = onNavigateToBin,
				onAddBookMark = { onRecorderAction(RecorderAction.AddBookMarkAction) },
				navigation = navigation
			)
		},
		snackbarHost = { SnackbarHost(snackBarHostState) },
		modifier = modifier,
	) { scPadding ->
		MicPermissionWrapper(
			modifier = Modifier.padding(
				start = dimensionResource(id = R.dimen.sc_padding),
				end = dimensionResource(R.dimen.sc_padding),
				top = dimensionResource(id = R.dimen.sc_padding_secondary) + scPadding.calculateTopPadding(),
				bottom = dimensionResource(id = R.dimen.sc_padding_secondary) + scPadding.calculateBottomPadding()
			)
		) {
			RecorderContent(
				timer = recorderTimer,
				recorderState = recorderState,
				recordingPointsCallback = deferredRecordingPoints,
				bookMarksDeferred = bookMarksSetDeferred,
				onRecorderAction = onRecorderAction,
				isStartRecordingEnabled = isRecorderReady,
				modifier = Modifier.fillMaxSize()
			)
		}
	}
}