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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.eva.feature_player.bookmarks.BookMarksViewModel
import com.eva.feature_player.bookmarks.BookmarksViewmodelFactory
import com.eva.feature_player.composable.ControllerLifeCycleObserver
import com.eva.feature_player.viewmodel.AudioPlayerViewModel
import com.eva.feature_player.viewmodel.PlayerViewmodelFactory
import com.eva.player_shared.PlayerMetadataViewmodel
import com.eva.player_shared.PlayerVisualizerViewmodel
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

		val route = backStackEntry.toRoute<PlayerSubGraph.AudioPlayerRoute>()

		val visualsViewModel = backStackEntry.sharedViewmodel<PlayerVisualizerViewmodel>(controller)
		val metaDataViewmodel = backStackEntry.sharedViewmodel<PlayerMetadataViewmodel>(controller)

		val visuals by visualsViewModel.fullVisualization.collectAsStateWithLifecycle()

		// route based viewmodel
		val bookmarkViewmodel = hiltViewModel<BookMarksViewModel, BookmarksViewmodelFactory>(
			creationCallback = { factory -> factory.create(route.audioId) },
		)
		val playerViewModel = hiltViewModel<AudioPlayerViewModel, PlayerViewmodelFactory>(
			creationCallback = { factory -> factory.create(route.audioId) },
		)

		val contentState by metaDataViewmodel.loadState.collectAsStateWithLifecycle()

		// player states
		val trackData by playerViewModel.trackData.collectAsStateWithLifecycle()
		val playerMetadata by playerViewModel.playerMetaData.collectAsStateWithLifecycle()
		val isControllerReady by playerViewModel.isControllerReady.collectAsStateWithLifecycle()

		// bookmarks state
		val bookMarkState by bookmarkViewmodel.bookmarkState.collectAsStateWithLifecycle()
		val bookMarks by bookmarkViewmodel.bookMarksFlow.collectAsStateWithLifecycle()

		ControllerLifeCycleObserver(
			audioId = route.audioId,
			onEvent = playerViewModel::onControllerEvents
		)

		// Handle UI Events
		UiEventsHandler(
			eventsFlow = {
				merge(
					metaDataViewmodel.uiEvent,
					visualsViewModel.uiEvent,
					playerViewModel.uiEvent,
					bookmarkViewmodel.uiEvent
				)
			},
			onNavigateBack = controller::popBackStack
		)

		val lifeCycleState by backStackEntry.lifecycle.currentStateFlow.collectAsStateWithLifecycle()

		CompositionLocalProvider(LocalSharedTransitionVisibilityScopeProvider provides this) {
			AudioPlayerScreenContainer(
				audioId = route.audioId,
				loadState = contentState,
				bookmarks = bookMarks,
				waveforms = { visuals },
				trackData = trackData,
				playerMetaData = playerMetadata,
				isControllerReady = isControllerReady,
				bookMarkState = bookMarkState,
				onFileEvent = metaDataViewmodel::onFileEvent,
				onPlayerEvents = playerViewModel::onPlayerEvents,
				onBookmarkEvent = bookmarkViewmodel::onBookMarkEvent,
				onNavigateToEdit = dropUnlessResumed {
					controller.navigate(PlayerSubGraph.AudioEditorRoute)
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

