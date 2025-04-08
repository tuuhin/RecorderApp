package com.eva.feature_player

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.eva.feature_player.composable.ControllerLifeCycleObserver
import com.eva.feature_player.viewmodel.AudioPlayerViewModel
import com.eva.feature_player.viewmodel.BookMarksViewModel
import com.eva.ui.R
import com.eva.ui.navigation.NavDialogs
import com.eva.ui.navigation.NavRoutes
import com.eva.ui.navigation.animatedComposable
import com.eva.ui.utils.LocalSharedTransitionVisibilityScopeProvider
import com.eva.ui.utils.UiEventsHandler
import com.eva.utils.NavDeepLinks

fun NavGraphBuilder.audioPlayerRoute(controller: NavHostController) =
	animatedComposable<NavRoutes.AudioPlayer>(
		deepLinks = listOf(
			navDeepLink {
				uriPattern = NavDeepLinks.PLAYER_DESTINATION_PATTERN
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
		UiEventsHandler(
			eventsFlow = viewModel::uiEvent,
			onNavigateBack = controller::popBackStack
		)

		// ui handler for bookmarks viewmodel
		UiEventsHandler(eventsFlow = bookMarksViewmodel::uiEvent)

		ControllerLifeCycleObserver(
			audioId = route.audioId,
			onEvent = viewModel::onControllerEvents
		)

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
					if (lifeCycleState.isAtLeast(state = Lifecycle.State.RESUMED)) {
						controller.navigate(NavRoutes.AudioEditor)
					}
				},
				onRenameItem = { audioId ->
					if (lifeCycleState.isAtLeast(Lifecycle.State.RESUMED)) {
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