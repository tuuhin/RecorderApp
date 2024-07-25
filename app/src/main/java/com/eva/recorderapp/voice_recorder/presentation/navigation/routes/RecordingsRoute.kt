package com.eva.recorderapp.voice_recorder.presentation.navigation.routes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavRoutes
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.UiEventsSideEffect
import com.eva.recorderapp.voice_recorder.presentation.recordings.RecordingsScreen
import com.eva.recorderapp.voice_recorder.presentation.recordings.RecordingsViewmodel

fun NavGraphBuilder.recordingsroute(
	controller: NavController
) = composable<NavRoutes.VoiceRecordings> {
	val viewModel = hiltViewModel<RecordingsViewmodel>()

	val recordings by viewModel.recordings.collectAsStateWithLifecycle()
	val isRecordingsLoaded by viewModel.isRecordingLoaded.collectAsStateWithLifecycle()
	val sortInfo by viewModel.sortInfo.collectAsStateWithLifecycle()
	val renameState by viewModel.renameState.collectAsStateWithLifecycle()

	UiEventsSideEffect(viewModel = viewModel)

	RecordingsScreen(
		isRecordingsLoaded = isRecordingsLoaded,
		recordings = recordings,
		sortInfo = sortInfo,
		renameState = renameState,
		onScreenEvent = viewModel::onScreenEvent,
		onRenameEvent = viewModel::onRenameRecordingEvent,
		onNavigateToBin = dropUnlessResumed {
			controller.navigate(NavRoutes.TrashRecordings)
		},
		navigation = {
			IconButton(
				onClick = dropUnlessResumed(block = controller::popBackStack)
			) {
				Icon(
					imageVector = Icons.AutoMirrored.Default.ArrowBack,
					contentDescription = stringResource(R.string.back_arrow)
				)
			}
		},
	)
}