package com.eva.recorderapp.voice_recorder.presentation.navigation.routes

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navDeepLink
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavDeepLinks
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavRoutes
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.UiEventsSideEffect
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.animatedComposable
import com.eva.recorderapp.voice_recorder.presentation.recorder.RecorderServiceBinder
import com.eva.recorderapp.voice_recorder.presentation.recorder.RecorderViewModel
import com.eva.recorderapp.voice_recorder.presentation.recorder.VoiceRecroderScreen

fun NavGraphBuilder.recorderRoute(
	navController: NavHostController
) = animatedComposable<NavRoutes.VoiceRecorder>(
	deepLinks = listOf(
		navDeepLink {
			uriPattern = NavDeepLinks.recorderDestinationPattern
			action = Intent.ACTION_VIEW
		},
	),
) {

	val viewModel = hiltViewModel<RecorderViewModel>()

	UiEventsSideEffect(viewModel = viewModel)

	RecorderServiceBinder { isBounded, service ->
		AnimatedContent(
			targetState = isBounded && service != null,
			label = "Setting the recorder animation",
			transitionSpec = { recorderServiceBinderTrasition() },
			modifier = Modifier.fillMaxSize()
		) { isReady ->
			if (isReady && service != null) {

				val timer by service.recorderTime.collectAsStateWithLifecycle()
				val recorderState by service.recorderState.collectAsStateWithLifecycle()
				val recorderAmplitude by service.amplitudes.collectAsStateWithLifecycle()

				VoiceRecroderScreen(
					stopWatch = timer,
					recorderState = recorderState,
					amplitudeCallback = { recorderAmplitude },
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
			} else Box(
				modifier = Modifier.fillMaxSize(),
				contentAlignment = Alignment.Center
			) {
				Text(
					text = stringResource(id = R.string.preparing_recorder),
					style = MaterialTheme.typography.titleMedium
				)
			}
		}
	}
}

fun AnimatedContentTransitionScope<Boolean>.recorderServiceBinderTrasition(
	scaleTranstion: FiniteAnimationSpec<Float> = spring(
		dampingRatio = Spring.DampingRatioLowBouncy,
		stiffness = Spring.StiffnessMedium
	),
	fadeTransition: FiniteAnimationSpec<Float> = tween(durationMillis = 200, easing = EaseInCubic)
): ContentTransform {
	return scaleIn(scaleTranstion) + fadeIn(fadeTransition) togetherWith
			scaleOut(scaleTranstion) + fadeOut(fadeTransition)
}