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
import com.eva.recorderapp.voice_recorder.presentation.settings.AudioSettingsScreen
import com.eva.recorderapp.voice_recorder.presentation.settings.AudioSettingsViewModel

fun NavGraphBuilder.audioSettingsRoute(
	controller: NavController
) = composable<NavRoutes.AudioSettings> {

	val viewModel = hiltViewModel<AudioSettingsViewModel>()

	val audioSettings by viewModel.audioSettings.collectAsStateWithLifecycle()
	val fileSettings by viewModel.fileSettings.collectAsStateWithLifecycle()

	AudioSettingsScreen(
		audioSettings = audioSettings,
		fileSettings = fileSettings,
		onAudioSettingsChange = viewModel::onAudioEvent,
		onFileSettingsChange = viewModel::onFileEvent,
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