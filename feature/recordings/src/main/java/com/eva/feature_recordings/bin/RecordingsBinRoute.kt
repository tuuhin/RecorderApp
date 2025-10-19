package com.eva.feature_recordings.bin

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.eva.feature_recordings.handlers.DeleteItemRequestHandler
import com.eva.ui.R
import com.eva.ui.navigation.NavRoutes
import com.eva.ui.navigation.animatedComposable
import com.eva.ui.utils.LocalSharedTransitionVisibilityScopeProvider
import com.eva.ui.utils.UiEventsHandler

fun NavGraphBuilder.trashRecordingsRoute(controller: NavController) =
	animatedComposable<NavRoutes.TrashRecordings> {

		val viewModel = hiltViewModel<RecordingsBinViewmodel>()

		DeleteItemRequestHandler(
			eventsFlow = viewModel::deleteRequestEvent,
			onResult = viewModel::onScreenEvent
		)

		UiEventsHandler(eventsFlow = viewModel::uiEvent)

		val recordings by viewModel.trashRecordings.collectAsStateWithLifecycle()
		val isLoaded by viewModel.isLoaded.collectAsStateWithLifecycle()

		CompositionLocalProvider(LocalSharedTransitionVisibilityScopeProvider provides this) {
			RecordingsBinScreen(
				isRecordingsLoaded = isLoaded,
				recordings = recordings,
				onScreenEvent = viewModel::onScreenEvent,
				navigation = {
					if (controller.previousBackStackEntry?.destination?.route != null) {
						IconButton(
							onClick = dropUnlessResumed(block = controller::popBackStack)
						) {
							Icon(
								imageVector = Icons.AutoMirrored.Default.ArrowBack,
								contentDescription = stringResource(id = R.string.back_arrow)
							)
						}
					}
				},
			)
		}
	}