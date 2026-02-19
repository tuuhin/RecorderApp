package com.eva.feature_settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.eva.feature_settings.screen.AudioSettingsScreen
import com.eva.feature_settings.screen.AudioSettingsViewModel
import com.eva.feature_settings.screen.STTModelsManagerViewModel
import com.eva.ui.R
import com.eva.ui.navigation.NavDialogs
import com.eva.ui.navigation.NavRoutes
import com.eva.ui.utils.LocalSharedTransitionVisibilityScopeProvider
import com.eva.ui.utils.UiEventsHandler
import kotlinx.coroutines.flow.merge

fun NavGraphBuilder.settingsRoute(controller: NavController) = composable<NavRoutes.AudioSettings> {

	val settingsViewModel = hiltViewModel<AudioSettingsViewModel>()
	val sttModelsViewmodel = hiltViewModel<STTModelsManagerViewModel>()

	val audioSettings by settingsViewModel.audioSettings.collectAsStateWithLifecycle()
	val fileSettings by settingsViewModel.fileSettings.collectAsStateWithLifecycle()
	val storageData by settingsViewModel.storageData.collectAsStateWithLifecycle()

	val sttModels by sttModelsViewmodel.modelsInfo.collectAsStateWithLifecycle()
	val transcriptionSettings by sttModelsViewmodel.transcriptionSettings.collectAsStateWithLifecycle()

	UiEventsHandler(eventsFlow = { merge(settingsViewModel.uiEvent, sttModelsViewmodel.uiEvent) })

	CompositionLocalProvider(LocalSharedTransitionVisibilityScopeProvider provides this) {
		AudioSettingsScreen(
			audioSettings = audioSettings,
			fileSettings = fileSettings,
			storageModel = storageData,
			sttModels = sttModels,
			transcriptionSettings = transcriptionSettings,
			onTranscriptionSettingChange = sttModelsViewmodel::onEvent,
			onAudioSettingsChange = settingsViewModel::onAudioEvent,
			onFileSettingsChange = settingsViewModel::onFileEvent,
			onNavigateToInfo = dropUnlessResumed {
				controller.navigate(NavDialogs.ApplicationInfo)
			},
			navigation = {
				if (controller.previousBackStackEntry?.destination?.route != null) {
					IconButton(
						onClick = dropUnlessResumed(block = controller::popBackStack)
					) {
						Icon(
							imageVector = Icons.AutoMirrored.Default.ArrowBack,
							contentDescription = stringResource(R.string.back_arrow)
						)
					}
				}
			},
		)
	}
}