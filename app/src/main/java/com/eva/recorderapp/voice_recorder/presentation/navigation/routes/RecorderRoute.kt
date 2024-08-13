package com.eva.recorderapp.voice_recorder.presentation.navigation.routes

import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavDeepLinks
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavRoutes
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.UiEventsSideEffect
import com.eva.recorderapp.voice_recorder.presentation.recorder.RecorderServiceBinder
import com.eva.recorderapp.voice_recorder.presentation.recorder.RecorderViewModel
import com.eva.recorderapp.voice_recorder.presentation.recorder.VoiceRecroderScreen

fun NavGraphBuilder.recorderRoute(
	navController: NavHostController
) = composable<NavRoutes.VoiceRecorder>(
	deepLinks = listOf(
		navDeepLink {
			uriPattern = NavDeepLinks.recorderDestinationPattern
			action = Intent.ACTION_VIEW
		},
	)
) {

	val viewModel = hiltViewModel<RecorderViewModel>()

	UiEventsSideEffect(viewModel = viewModel)

	RecorderServiceBinder { isBounded, service ->
		Crossfade(
			targetState = isBounded && service != null,
			animationSpec = tween(20, easing = LinearEasing),
			label = "Setting the recorder animation"
		) { isReady ->
			if (isReady && service != null) {

				val timer by service.recorderTime.collectAsStateWithLifecycle()
				val recorderstate by service.recorderState.collectAsStateWithLifecycle()
				val recorderAmplitude by service.amplitides.collectAsStateWithLifecycle()

				VoiceRecroderScreen(
					stopWatch = timer,
					recorderState = recorderstate,
					recorderAmps = recorderAmplitude,
					onRecorderAction = viewModel::onAction,
					onShowRecordings = dropUnlessResumed {
						navController.navigate(NavRoutes.VoiceRecordings)
					},
					onNavigateToBin = dropUnlessResumed {
						navController.navigate(NavRoutes.TrashRecordings)
					},
					onNavigateToSettings = dropUnlessResumed {
						navController.navigate(NavRoutes.AudioSettings)
					},
				)
			} else Box(modifier = Modifier.fillMaxSize())

		}

	}
}
