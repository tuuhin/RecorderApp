package com.eva.recorderapp.voice_recorder.presentation.navigation.routes

import android.content.Intent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navDeepLink
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavDeepLinks
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavRoutes
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.UiEventsSideEffect
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.animatedComposable
import com.eva.recorderapp.voice_recorder.presentation.recorder.RecorderViewModel
import com.eva.recorderapp.voice_recorder.presentation.recorder.VoiceRecorderScreen
import com.eva.recorderapp.voice_recorder.presentation.util.LocalSharedTransitionVisibilityScopeProvider

fun NavGraphBuilder.recorderRoute(
	navController: NavHostController,
) = animatedComposable<NavRoutes.VoiceRecorder>(
	deepLinks = listOf(
		navDeepLink {
			uriPattern = NavDeepLinks.recorderDestinationPattern
			action = Intent.ACTION_VIEW
		},
	),
) {

	val viewModel = hiltViewModel<RecorderViewModel>()

	UiEventsSideEffect(eventsFlow = viewModel::uiEvent)

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