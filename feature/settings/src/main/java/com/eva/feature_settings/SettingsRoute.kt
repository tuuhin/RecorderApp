package com.eva.feature_settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.eva.feature_settings.screen.AudioSettingsScreen
import com.eva.feature_settings.screen.AudioSettingsViewModel
import com.eva.ui.R
import com.eva.ui.navigation.NavDialogs
import com.eva.ui.navigation.NavRoutes
import com.eva.ui.navigation.animatedComposable
import com.eva.ui.utils.LocalSharedTransitionVisibilityScopeProvider
import com.eva.ui.utils.UiEventsHandler

fun NavGraphBuilder.settingsRoute(controller: NavController) =
	animatedComposable<NavRoutes.AudioSettings> {

		val viewModel = hiltViewModel<AudioSettingsViewModel>()

		val audioSettings by viewModel.audioSettings.collectAsStateWithLifecycle()
		val fileSettings by viewModel.fileSettings.collectAsStateWithLifecycle()
		val storageData by viewModel.storageData.collectAsStateWithLifecycle()

		UiEventsHandler(eventsFlow = viewModel::uiEvent)

		CompositionLocalProvider(LocalSharedTransitionVisibilityScopeProvider provides this) {
			AudioSettingsScreen(
				audioSettings = audioSettings,
				fileSettings = fileSettings,
				storageModel = storageData,
				onAudioSettingsChange = viewModel::onAudioEvent,
				onFileSettingsChange = viewModel::onFileEvent,
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