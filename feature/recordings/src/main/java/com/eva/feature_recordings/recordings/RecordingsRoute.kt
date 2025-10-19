package com.eva.feature_recordings.recordings

import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navDeepLink
import com.eva.feature_recordings.handlers.TrashItemRequestHandler
import com.eva.ui.R
import com.eva.ui.navigation.NavDialogs
import com.eva.ui.navigation.NavRoutes
import com.eva.ui.navigation.PlayerSubGraph
import com.eva.ui.navigation.animatedComposable
import com.eva.ui.utils.LocalSharedTransitionVisibilityScopeProvider
import com.eva.ui.utils.UiEventsHandler
import com.eva.utils.NavDeepLinks

fun NavGraphBuilder.recordingsRoute(controller: NavController) =
	animatedComposable<NavRoutes.VoiceRecordings>(
		deepLinks = listOf(
			navDeepLink {
				uriPattern = NavDeepLinks.RECORDING_DESTINATION_PATTERN
				action = Intent.ACTION_VIEW
			},
		),
	) {

		val lifecycleOwner = LocalLifecycleOwner.current

		val viewModel = hiltViewModel<RecordingsViewmodel>()

		TrashItemRequestHandler(
			eventsFlow = viewModel::trashRequestEvent,
			onResult = viewModel::onScreenEvent
		)

		UiEventsHandler(eventsFlow = viewModel::uiEvent)

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
				onNavigateToSearch = dropUnlessResumed {
					controller.navigate(NavRoutes.SearchRecordings)
				},
				onNavigationToCategories = dropUnlessResumed {
					controller.navigate(NavRoutes.ManageCategories)
				},
				onRecordingSelect = { record ->
					if (lifeCycleState.isAtLeast(Lifecycle.State.RESUMED)) {
						val audioRoute = PlayerSubGraph.NavGraph(record.id)
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