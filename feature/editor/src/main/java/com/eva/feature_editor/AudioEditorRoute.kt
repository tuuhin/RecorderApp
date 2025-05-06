package com.eva.feature_editor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.eva.editor.domain.model.AudioClipConfig
import com.eva.editor.domain.model.AudioEditAction
import com.eva.feature_editor.viewmodel.AudioEditorViewModel
import com.eva.feature_editor.viewmodel.EditorViewmodelFactory
import com.eva.player_shared.PlayerMetadataViewmodel
import com.eva.player_shared.PlayerVisualizerViewmodel
import com.eva.player_shared.util.PlayerGraphData
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.ui.R
import com.eva.ui.navigation.PlayerSubGraph
import com.eva.ui.navigation.animatedComposable
import com.eva.ui.utils.UiEventsHandler
import com.eva.ui.utils.sharedViewmodel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.merge

fun NavGraphBuilder.audioEditorRoute(controller: NavController) =
	animatedComposable<PlayerSubGraph.AudioEditorRoute> { backstackEntry ->

		val sharedViewmodel = backstackEntry.sharedViewmodel<PlayerMetadataViewmodel>(controller)
		val visualizerViewmodel =
			backstackEntry.sharedViewmodel<PlayerVisualizerViewmodel>(controller)

		val loadState by sharedViewmodel.loadState.collectAsStateWithLifecycle()
		val compressedVisuals by visualizerViewmodel.compressedVisuals.collectAsStateWithLifecycle()

		// ui events handler
		UiEventsHandler(
			eventsFlow = {
				merge(sharedViewmodel.uiEvent, visualizerViewmodel.uiEvent)
			},
		)

		AudioEditorScreenContainer(
			loadState = loadState,
			content = { model ->
				AudioEditorScreenStateful(
					fileModel = model,
					visualization = { compressedVisuals },
					onClipDataUpdate = visualizerViewmodel::updateClipConfigs
				)
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

@Composable
fun AudioEditorScreenStateful(
	fileModel: AudioFileModel,
	visualization: PlayerGraphData,
	onClipDataUpdate: (List<Pair<AudioClipConfig, AudioEditAction>>) -> Unit,
	modifier: Modifier = Modifier
) {

	val lifecyleOwner = LocalLifecycleOwner.current
	val currentOnClipDataUpdate by rememberUpdatedState(onClipDataUpdate)

	val viewModel = hiltViewModel<AudioEditorViewModel, EditorViewmodelFactory>(
		creationCallback = { factory -> factory.create(fileModel) },
	)

	UiEventsHandler(eventsFlow = viewModel::uiEvent)

	LaunchedEffect(lifecyleOwner) {
		viewModel.clipConfigs.collectLatest {
			currentOnClipDataUpdate(it)
		}
	}

	val isPlaying by viewModel.isPlayerPlaying.collectAsStateWithLifecycle()
	val trackData by viewModel.trackData.collectAsStateWithLifecycle()
	val clipConfig by viewModel.clipConfig.collectAsStateWithLifecycle()

	AudioEditorScreenContent(
		fileModel = fileModel,
		isPlaying = isPlaying,
		clipConfig = clipConfig,
		trackData = trackData,
		graphData = visualization,
		onEvent = viewModel::onEvent,
		modifier = modifier,
	)
}