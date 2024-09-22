package com.eva.recorderapp.voice_recorder.presentation.navigation.routes

import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavDeepLinks
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavDialogs
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavRoutes
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.UiEventsSideEffect
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.animatedComposable
import com.eva.recorderapp.voice_recorder.presentation.record_player.AudioPlayerScreen
import com.eva.recorderapp.voice_recorder.presentation.record_player.AudioPlayerViewModel
import com.eva.recorderapp.voice_recorder.presentation.record_player.composable.ControllerLifeCyleObserver

fun NavGraphBuilder.audioPlayerRoute(
	controller: NavHostController,
) = animatedComposable<NavRoutes.AudioPlayer>(
	deepLinks = listOf(
		navDeepLink {
			uriPattern = NavDeepLinks.appPlayerDestinationPattern
			action = Intent.ACTION_VIEW
		},
	),
) { backStackEntry ->

	val route = backStackEntry.toRoute<NavRoutes.AudioPlayer>()
	val lifecycleOwner = LocalLifecycleOwner.current

	val viewModel = hiltViewModel<AudioPlayerViewModel>()

	val contentState by viewModel.loadState.collectAsStateWithLifecycle()
	val playerState by viewModel.currentAudioState.collectAsStateWithLifecycle()

	// lifeCycleState
	val lifeCycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()

	UiEventsSideEffect(eventsFlow = viewModel::uiEvent)

	ControllerLifeCyleObserver(audioId = route.audioId, onEvent = viewModel::onControllerEvents)

	AudioPlayerScreen(
		loadState = contentState,
		playerState = playerState,
		onPlayerEvents = viewModel::onPlayerEvents,
		onNavigateToEdit = dropUnlessResumed {
			controller.navigate(NavRoutes.AudioEditor)
		},
		onRenameItem = { audioId ->
			if (lifeCycleState.isAtLeast(State.RESUMED)) {
				val dialog = NavDialogs.RenameRecordingDialog(audioId)
				controller.navigate(dialog)
			}
		},
		navigation = {
			if (controller.previousBackStackEntry?.destination?.route != null) {
				IconButton(
					onClick = dropUnlessResumed(block = controller::popBackStack),
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