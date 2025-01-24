package com.eva.recorderapp.voice_recorder.presentation.navigation.routes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavRoutes
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.UiEventsSideEffect
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.animatedComposable
import com.eva.recorderapp.voice_recorder.presentation.recordings.search.SearchRecordingsScreen
import com.eva.recorderapp.voice_recorder.presentation.recordings.search.SearchRecordingsViewmodel
import com.eva.recorderapp.voice_recorder.presentation.util.LocalSharedTransitionVisibilityScopeProvider

fun NavGraphBuilder.recordingsSearchRoute(
	controller: NavController,
) = animatedComposable<NavRoutes.SearchRecordings> {

	val lifecycleOwner = LocalLifecycleOwner.current

	val viewModel = hiltViewModel<SearchRecordingsViewmodel>()

	val categories by viewModel.categories.collectAsStateWithLifecycle()
	val searchResults by viewModel.recordings.collectAsStateWithLifecycle()
	val screenState by viewModel.searchState.collectAsStateWithLifecycle()

	// lifeCycleState
	val lifeCycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()

	UiEventsSideEffect(eventsFlow = viewModel::uiEvent)

	CompositionLocalProvider(LocalSharedTransitionVisibilityScopeProvider provides this) {
		SearchRecordingsScreen(
			state = screenState,
			categories = categories,
			searchResults = searchResults,
			onEvent = viewModel::onEvent,
			onSelectRecording = { record ->
				if (lifeCycleState.isAtLeast(Lifecycle.State.RESUMED)) {
					val audioRoute = NavRoutes.AudioPlayer(record.id)
					controller.navigate(audioRoute)
				}
			},
			navigation = {
				if (controller.previousBackStackEntry?.destination?.route != null) {
					IconButton(
						onClick = dropUnlessResumed(block = controller::popBackStack)
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