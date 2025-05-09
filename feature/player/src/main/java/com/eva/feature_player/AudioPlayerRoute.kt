package com.eva.feature_player

import android.content.Intent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.eva.player_shared.util.PlayerGraphData
import com.eva.recordings.domain.models.AudioFileModel
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

		val metaDataViewmodel = backStackEntry.sharedViewmodel<PlayerMetadataViewmodel>(controller)
		val visualizerViewModel =
			backStackEntry.sharedViewmodel<PlayerVisualizerViewmodel>(controller)

		val contentState by metaDataViewmodel.loadState.collectAsStateWithLifecycle()
		val lifeCycleState by backStackEntry.lifecycle.currentStateFlow.collectAsStateWithLifecycle()

		val visuals by visualizerViewModel.fullVisualization.collectAsStateWithLifecycle()

		// Handle UI Events
		UiEventsHandler(
			eventsFlow = { merge(metaDataViewmodel.uiEvent, visualizerViewModel.uiEvent) },
			onNavigateBack = controller::popBackStack
		)

		CompositionLocalProvider(LocalSharedTransitionVisibilityScopeProvider provides this) {
			AudioPlayerScreenContainer(
				audioId = route.audioId,
				loadState = contentState,
				onFileEvent = metaDataViewmodel::onFileEvent,
				content = { model ->
					AudioPlayerContentStateFul(
						model = model,
						visualization = { visuals },
					)
				},
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


@Composable
private fun AudioPlayerContentStateFul(
	model: AudioFileModel,
	visualization: PlayerGraphData,
	modifier: Modifier = Modifier
) {
	val playerViewModel = hiltViewModel<AudioPlayerViewModel, PlayerViewmodelFactory>(
		creationCallback = { factory -> factory.create(model) },
	)

	val bookmarkViewmodel = hiltViewModel<BookMarksViewModel, BookmarksViewmodelFactory>(
		creationCallback = { factory -> factory.create(model) },
	)

	// player states
	val trackData by playerViewModel.trackData.collectAsStateWithLifecycle()
	val playerMetadata by playerViewModel.playerMetaData.collectAsStateWithLifecycle()
	val isControllerReady by playerViewModel.isControllerReady.collectAsStateWithLifecycle()

	// bookmarks state
	val bookMarkState by bookmarkViewmodel.bookmarkState.collectAsStateWithLifecycle()
	val bookMarks by bookmarkViewmodel.bookMarksFlow.collectAsStateWithLifecycle()

	ControllerLifeCycleObserver(
		audioId = model.id,
		onEvent = playerViewModel::onControllerEvents
	)

	UiEventsHandler(
		eventsFlow = { merge(playerViewModel.uiEvent, bookmarkViewmodel.uiEvent) },
	)

	AudioPlayerScreenContent(
		fileModel = model,
		bookmarks = bookMarks,
		waveforms = visualization,
		trackData = trackData,
		playerMetaData = playerMetadata,
		isControllerReady = isControllerReady,
		bookMarkState = bookMarkState,
		onPlayerEvents = playerViewModel::onPlayerEvents,
		onBookmarkEvent = bookmarkViewmodel::onBookMarkEvent,
		modifier = modifier,
	)
}