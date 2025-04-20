package com.eva.feature_editor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.eva.ui.navigation.animatedComposable
import com.eva.ui.utils.UiEventsHandler
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.dropUnlessResumed
import com.eva.player_shared.PlayerMetadataViewmodel
import com.eva.ui.R
import com.eva.ui.navigation.PlayerSubGraph
import com.eva.ui.utils.sharedViewmodel
import kotlinx.coroutines.flow.merge

fun NavGraphBuilder.audioEditorRoute(controller: NavController) =
	animatedComposable<PlayerSubGraph.AudioEditorRoute> { backstackEntry ->

		val sharedViewmodel = backstackEntry.sharedViewmodel<PlayerMetadataViewmodel>(controller)

		val viewModel = hiltViewModel<AudioEditorViewModel>()

		val isPlaying by viewModel.isPlayerPlaying.collectAsStateWithLifecycle()
		val trackData by viewModel.trackData.collectAsStateWithLifecycle()

		val loadState by sharedViewmodel.loadState.collectAsStateWithLifecycle()

		// ui events handler
		UiEventsHandler(
			eventsFlow = { merge(viewModel.uiEvent, sharedViewmodel.uiEvent) },
		)

		AudioEditorScreen(
			loadState = loadState,
			track = trackData,
			isPlaying = isPlaying,
			onEvent = viewModel::onEvent,
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