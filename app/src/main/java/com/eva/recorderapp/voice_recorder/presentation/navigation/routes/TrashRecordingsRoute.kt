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
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavRoutes
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.UiEventsSideEffect
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.animatedComposable
import com.eva.recorderapp.voice_recorder.presentation.recordings.RecordingsBinScreen
import com.eva.recorderapp.voice_recorder.presentation.recordings.RecordingsBinViewmodel

fun NavGraphBuilder.trashRecordingsRoute(
	controller: NavController
) = animatedComposable<NavRoutes.TrashRecordings> {

	val viewModel = hiltViewModel<RecordingsBinViewmodel>()

	val recordings by viewModel.trashRecordings.collectAsStateWithLifecycle()
	val isLoaded by viewModel.isLoaded.collectAsStateWithLifecycle()

	UiEventsSideEffect(viewModel = viewModel)

	RecordingsBinScreen(
		isRecordingsLoaded = isLoaded,
		recordings = recordings,
		onScreenEvent = viewModel::onScreenEvent,
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