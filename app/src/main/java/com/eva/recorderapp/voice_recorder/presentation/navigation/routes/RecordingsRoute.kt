package com.eva.recorderapp.voice_recorder.presentation.navigation.routes

import android.content.Intent
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
import androidx.navigation.navDeepLink
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavDeepLinks
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavDialogs
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavRoutes
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.UiEventsSideEffect
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.animatedComposable
import com.eva.recorderapp.voice_recorder.presentation.recordings.RecordingsScreen
import com.eva.recorderapp.voice_recorder.presentation.recordings.RecordingsViewmodel
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.handlers.TrashRecordingsRequestHandler
import com.eva.recorderapp.voice_recorder.presentation.util.LocalSharedTransitionVisibilityScopeProvider

fun NavGraphBuilder.recordingsRoute(
	controller: NavController,
) = animatedComposable<NavRoutes.VoiceRecordings>(
	deepLinks = listOf(
		navDeepLink {
			uriPattern = NavDeepLinks.recordingsDestinationPattern
			action = Intent.ACTION_VIEW
		},
	),
) {

	val lifecycleOwner = LocalLifecycleOwner.current

	val viewModel = hiltViewModel<RecordingsViewmodel>()

	TrashRecordingsRequestHandler(
		eventsFlow = viewModel::trashRequestEvent,
		onResult = viewModel::onScreenEvent
	)

	UiEventsSideEffect(eventsFlow = viewModel::uiEvent)

	val recordings by viewModel.recordings.collectAsStateWithLifecycle()
	val isRecordingsLoaded by viewModel.isLoaded.collectAsStateWithLifecycle()
	val sortInfo by viewModel.sortInfo.collectAsStateWithLifecycle()
	val categories by viewModel.categories.collectAsStateWithLifecycle()
	val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

	// lifeCycleState
	val lifeCycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()

	CompositionLocalProvider(LocalSharedTransitionVisibilityScopeProvider provides this) {
		RecordingsScreen(
			isRecordingsLoaded = isRecordingsLoaded,
			recordings = recordings,
			categories = categories,
			sortInfo = sortInfo,
			selectedCategory = selectedCategory,
			onScreenEvent = viewModel::onScreenEvent,
			onNavigateToBin = dropUnlessResumed {
				controller.navigate(NavRoutes.TrashRecordings)
			},
			onNavigationToCategories = dropUnlessResumed {
				controller.navigate(NavRoutes.ManageCategories)
			},
			onRecordingSelect = { record ->
				if (lifeCycleState.isAtLeast(Lifecycle.State.RESUMED)) {
					val audioRoute = NavRoutes.AudioPlayer(record.id)
					controller.navigate(audioRoute)
				}
			},
			onShowRenameDialog = { record ->
				if (lifeCycleState.isAtLeast(Lifecycle.State.RESUMED) && record != null) {
					val dialog = NavDialogs.RenameRecordingDialog(record.id)
					controller.navigate(dialog)
				}
			},
			onMoveToCategory = { collection ->
				if (lifeCycleState.isAtLeast(Lifecycle.State.RESUMED) && collection.isNotEmpty()) {
					val recordingIds = collection.map { it.id }
					val route = NavRoutes.SelectRecordingCategoryRoute(recordingIds)
					controller.navigate(route)
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