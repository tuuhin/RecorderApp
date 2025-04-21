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
import com.eva.feature_player.bookmarks.BookMarksViewModel
import com.eva.feature_player.composable.ControllerLifeCycleObserver
import com.eva.feature_player.viewmodel.AudioPlayerViewModel
import com.eva.player_shared.PlayerMetadataViewmodel
import com.eva.ui.R
import com.eva.ui.navigation.NavDialogs
import com.eva.ui.navigation.PlayerSubGraph
import com.eva.ui.navigation.animatedComposable
import com.eva.ui.utils.LocalSharedTransitionVisibilityScopeProvider
import com.eva.ui.utils.UiEventsHandler
import com.eva.ui.utils.sharedViewmodel
import com.eva.utils.NavDeepLinks
import kotlinx.coroutines.flow.merge

fun NavGraphBuilder.audioPlayerRoute(controller: NavHostController) =
	animatedComposable<PlayerSubGraph.AudioPlayerRoute>(
		deepLinks = listOf(
			navDeepLink {
				uriPattern = NavDeepLinks.PLAYER_DESTINATION_PATTERN
				action = Intent.ACTION_VIEW
			},
		),
		sizeTransform = { SizeTransform(clip = false) { _, _ -> tween(durationMillis = 300) } }
	) { backStackEntry ->

		val sharedViewModel = backStackEntry.sharedViewmodel<PlayerMetadataViewmodel>(controller)

		val playerViewModel = hiltViewModel<AudioPlayerViewModel>()
		val bookMarksViewmodel = hiltViewModel<BookMarksViewModel>()

		//shared state
		val contentState by sharedViewModel.loadState.collectAsStateWithLifecycle()

		//player state
		val playerState by playerViewModel.currentAudioState.collectAsStateWithLifecycle()
		val waveforms by playerViewModel.waveforms.collectAsStateWithLifecycle()

		// bookmarks state
		val createOrEditBookMarkState by bookMarksViewmodel.bookmarkState.collectAsStateWithLifecycle()
		val bookMarks by bookMarksViewmodel.bookMarksFlow.collectAsStateWithLifecycle()

		// lifeCycleState
		val lifeCycleState by backStackEntry.lifecycle.currentStateFlow.collectAsStateWithLifecycle()

		// Handle UI Events
		UiEventsHandler(
			eventsFlow = {
				merge(
					playerViewModel.uiEvent,
					sharedViewModel.uiEvent,
					bookMarksViewmodel.uiEvent
				)
			},
			onNavigateBack = controller::popBackStack
		)

		ControllerLifeCycleObserver(onEvent = playerViewModel::onControllerEvents)

		CompositionLocalProvider(LocalSharedTransitionVisibilityScopeProvider provides this) {
			AudioPlayerScreen(
				selectedAudioId = sharedViewModel.audioId,
				waveforms = { waveforms },
				loadState = contentState,
				playerState = playerState,
				bookmarks = bookMarks,
				bookMarkState = createOrEditBookMarkState,
				onPlayerEvents = playerViewModel::onPlayerEvents,
				onBookmarkEvent = bookMarksViewmodel::onBookMarkEvent,
				onFileEvent = sharedViewModel::onFileEvent,
				onNavigateToEdit = dropUnlessResumed {
					if (lifeCycleState.isAtLeast(state = Lifecycle.State.RESUMED)) {
						controller.navigate(PlayerSubGraph.AudioEditorRoute)
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