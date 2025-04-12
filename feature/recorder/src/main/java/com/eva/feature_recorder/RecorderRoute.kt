package com.eva.feature_recorder

import android.content.Intent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navDeepLink
import com.eva.feature_recorder.screen.RecorderViewModel
import com.eva.feature_recorder.screen.VoiceRecorderScreen
import com.eva.ui.navigation.NavRoutes
import com.eva.ui.navigation.animatedComposable
import com.eva.ui.utils.LocalSharedTransitionVisibilityScopeProvider
import com.eva.ui.utils.UiEventsHandler
import com.eva.utils.NavDeepLinks

fun NavGraphBuilder.recorderRoute(navController: NavHostController) =
	animatedComposable<NavRoutes.VoiceRecorder>(
		deepLinks = listOf(
			navDeepLink {
				uriPattern = NavDeepLinks.RECORDER_DESTINATION_PATTERN
				action = Intent.ACTION_VIEW
			},
		),
	) {

		val viewModel = hiltViewModel<RecorderViewModel>()

		UiEventsHandler(eventsFlow = viewModel::uiEvent)

		CompositionLocalProvider(
			LocalSharedTransitionVisibilityScopeProvider provides this
		) {
			VoiceRecorderScreen(
				onRecorderAction = viewModel::onAction,
				onShowRecordings = dropUnlessResumed { navController.navigate(NavRoutes.VoiceRecordings) },
				onNavigateToBin = dropUnlessResumed { navController.navigate(NavRoutes.TrashRecordings) },
				onNavigateToSettings = dropUnlessResumed { navController.navigate(NavRoutes.AudioSettings) },
			)
		}
	}