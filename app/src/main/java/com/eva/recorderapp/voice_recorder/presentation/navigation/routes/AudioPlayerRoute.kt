package com.eva.recorderapp.voice_recorder.presentation.navigation.routes

import android.content.Intent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle.State
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
import com.eva.recorderapp.voice_recorder.presentation.record_player.BookMarksViewModel
import com.eva.recorderapp.voice_recorder.presentation.record_player.composable.ControllerLifeCycleObserver
import com.eva.recorderapp.voice_recorder.presentation.util.LocalSharedTransitionVisibilityScopeProvider

fun NavGraphBuilder.audioPlayerRoute(
	controller: NavHostController,
) = animatedComposable<NavRoutes.AudioPlayer>(
	deepLinks = listOf(
		navDeepLink {
			uriPattern = NavDeepLinks.appPlayerDestinationPattern
			action = Intent.ACTION_VIEW
		},
	),
	sizeTransform = { SizeTransform(clip = false) { _, _ -> tween(durationMillis = 300) } }
) { backStackEntry ->

	val route = backStackEntry.toRoute<NavRoutes.AudioPlayer>()

	val viewModel = hiltViewModel<AudioPlayerViewModel>()
	val bookMarksViewmodel = hiltViewModel<BookMarksViewModel>()

	//player state
	val contentState by viewModel.loadState.collectAsStateWithLifecycle()
	val playerState by viewModel.currentAudioState.collectAsStateWithLifecycle()
	val waveforms by viewModel.waveforms.collectAsStateWithLifecycle()

	// bookmarks state
	val createOrEditBookMarkState by bookMarksViewmodel.bookmarkState.collectAsStateWithLifecycle()
	val bookMarks by bookMarksViewmodel.bookMarksFlow.collectAsStateWithLifecycle()

	// lifeCycleState
	val lifeCycleState by backStackEntry.lifecycle.currentStateFlow.collectAsStateWithLifecycle()

	// ui handler for player viewmodel
	UiEventsSideEffect(
		eventsFlow = viewModel::uiEvent,
		onPopScreenEvent = controller::popBackStack
	)

	// ui handler for bookmarks viewmodel
	UiEventsSideEffect(eventsFlow = bookMarksViewmodel::uiEvent)

	ControllerLifeCycleObserver(audioId = route.audioId, onEvent = viewModel::onControllerEvents)

	CompositionLocalProvider(LocalSharedTransitionVisibilityScopeProvider provides this) {
		AudioPlayerScreen(
			selectedAudioId = route.audioId,
			waveforms = { waveforms },
			loadState = contentState,
			playerState = playerState,
			bookmarks = bookMarks,
			bookMarkState = createOrEditBookMarkState,
			onPlayerEvents = viewModel::onPlayerEvents,
			onBookmarkEvent = bookMarksViewmodel::onBookMarkEvent,
			onFileEvent = viewModel::onFileEvents,
			onNavigateToEdit = dropUnlessResumed {
				if (lifeCycleState.isAtLeast(State.RESUMED)) {
					controller.navigate(NavRoutes.AudioEditor)
				}
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
}