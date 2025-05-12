package com.eva.feature_editor

import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.eva.editor.domain.AudioConfigToActionList
import com.eva.feature_editor.viewmodel.AudioEditorViewModel
import com.eva.feature_editor.viewmodel.EditorViewmodelFactory
import com.eva.player_shared.PlayerMetadataViewmodel
import com.eva.player_shared.PlayerVisualizerViewmodel
import com.eva.player_shared.util.PlayerGraphData
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.ui.R
import com.eva.ui.navigation.NavRoutes
import com.eva.ui.navigation.PlayerSubGraph
import com.eva.ui.navigation.animatedComposable
import com.eva.ui.utils.UiEventsHandler
import com.eva.ui.utils.sharedViewmodel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.merge

fun NavGraphBuilder.audioEditorRoute(controller: NavController) =
	animatedComposable<PlayerSubGraph.AudioEditorRoute>(
		sizeTransform = {
			SizeTransform(clip = false) { _, _ -> tween(durationMillis = 300) }
		},
	) { backstackEntry ->

		val sharedViewmodel = backstackEntry.sharedViewmodel<PlayerMetadataViewmodel>(controller)
		val visualsViewmodel = backstackEntry.sharedViewmodel<PlayerVisualizerViewmodel>(controller)

		val loadState by sharedViewmodel.loadState.collectAsStateWithLifecycle()
		val compressedVisuals by visualsViewmodel.compressedVisuals.collectAsStateWithLifecycle()

		// ui events handler
		UiEventsHandler(
			eventsFlow = { merge(sharedViewmodel.uiEvent, visualsViewmodel.uiEvent) },
		)

		AudioEditorScreenContainer(
			loadState = loadState,
			content = { model ->
				AudioEditorScreenStateful(
					fileModel = model,
					visualization = { compressedVisuals },
					onClipDataUpdate = visualsViewmodel::updateClipConfigs,
					onExportStarted = dropUnlessResumed {
						controller.navigate(NavRoutes.VoiceRecordings) {
							popUpTo<NavRoutes.VoiceRecordings> {
								inclusive = true
							}
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
			},
		)
	}

@Composable
fun AudioEditorScreenStateful(
	fileModel: AudioFileModel,
	visualization: PlayerGraphData,
	onClipDataUpdate: (AudioConfigToActionList) -> Unit,
	onExportStarted: () -> Unit,
	modifier: Modifier = Modifier,
	navigation: @Composable () -> Unit = {},
) {

	val lifecyleOwner = LocalLifecycleOwner.current
	val currentOnClipDataUpdate by rememberUpdatedState(onClipDataUpdate)
	val currentOnExportStarted by rememberUpdatedState(onExportStarted)

	val viewModel = hiltViewModel<AudioEditorViewModel, EditorViewmodelFactory>(
		creationCallback = { factory -> factory.create(fileModel) },
	)

	UiEventsHandler(eventsFlow = viewModel::uiEvent)

	LaunchedEffect(lifecyleOwner) {
		viewModel.clipConfigs.collectLatest {
			currentOnClipDataUpdate(it)
		}
	}

	LaunchedEffect(lifecyleOwner) {
		viewModel.exportBegun.collectLatest {
			currentOnExportStarted()
		}
	}

	val isPlaying by viewModel.isPlayerPlaying.collectAsStateWithLifecycle()
	val trackData by viewModel.trackData.collectAsStateWithLifecycle()
	val clipConfig by viewModel.clipConfig.collectAsStateWithLifecycle()
	val transformationState by viewModel.transformationState.collectAsStateWithLifecycle()
	val undoRedoState by viewModel.undoRedoState.collectAsStateWithLifecycle()

	val totalConfigs by viewModel.clipConfigs.collectAsStateWithLifecycle()
	val isMediaEdited by remember(totalConfigs) {
		derivedStateOf { totalConfigs.count() >= 1 }
	}

	AudioEditorScreenContent(
		graphData = visualization,
		isPlaying = isPlaying,
		clipConfig = clipConfig,
		trackData = trackData,
		isMediaEdited = isMediaEdited,
		undoRedoState = undoRedoState,
		transformationState = transformationState,
		onEvent = viewModel::onEvent,
		modifier = modifier,
		navigation = navigation,
	)
}