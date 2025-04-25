package com.eva.feature_editor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.eva.feature_editor.viewmodel.AudioEditorViewModel
import com.eva.feature_editor.viewmodel.EditorViewmodelFactory
import com.eva.player_shared.PlayerMetadataViewmodel
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.ui.R
import com.eva.ui.navigation.PlayerSubGraph
import com.eva.ui.navigation.animatedComposable
import com.eva.ui.utils.UiEventsHandler
import com.eva.ui.utils.sharedViewmodel

fun NavGraphBuilder.audioEditorRoute(controller: NavController) =
	animatedComposable<PlayerSubGraph.AudioEditorRoute> { backstackEntry ->

		val sharedViewmodel = backstackEntry.sharedViewmodel<PlayerMetadataViewmodel>(controller)

		val loadState by sharedViewmodel.loadState.collectAsStateWithLifecycle()

		// ui events handler
		UiEventsHandler(eventsFlow = sharedViewmodel::uiEvent)

		AudioEditorScreenContainer(
			loadState = loadState,
			content = { model -> AudioEditorScreenStateful(model) },
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

@Composable
fun AudioEditorScreenStateful(fileModel: AudioFileModel, modifier: Modifier = Modifier) {

	val editorViewModel = hiltViewModel<AudioEditorViewModel, EditorViewmodelFactory>(
		creationCallback = { factory -> factory.create(fileModel) },
	)

	UiEventsHandler(eventsFlow = editorViewModel::uiEvent)

	val isPlaying by editorViewModel.isPlayerPlaying.collectAsStateWithLifecycle()
	val trackData by editorViewModel.trackData.collectAsStateWithLifecycle()
	val clipConfig by editorViewModel.clipConfig.collectAsStateWithLifecycle()
	val visualization by editorViewModel.visuals.collectAsStateWithLifecycle()

	AudioEditorScreenContent(
		fileModel = fileModel,
		isPlaying = isPlaying,
		clipConfig = clipConfig,
		trackData = trackData,
		graphData = { visualization },
		onEvent = editorViewModel::onEvent,
		modifier = modifier,
	)
}