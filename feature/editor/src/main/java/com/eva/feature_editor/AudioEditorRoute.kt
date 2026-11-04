package com.eva.feature_editor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import com.eva.feature_editor.viewmodel.AudioEditorViewModel
import com.eva.feature_editor.viewmodel.EditorViewmodelFactory
import com.eva.player_shared.PlayerMetadataViewmodel
import com.eva.player_shared.PlayerVisualizerViewmodel
import com.eva.player_shared.factory.PlayerMetadataViewmodelFactory
import com.eva.player_shared.factory.PlayerVisualizerViewmodelFactory
import com.eva.ui.R
import com.eva.ui.navigation.NavRoutes
import com.eva.ui.navigation.PlayerSubGraph
import com.eva.ui.navigation.animatedComposable
import com.eva.ui.utils.LocalSharedTransitionVisibilityScopeProvider
import com.eva.ui.utils.UiEventsHandler
import com.eva.ui.utils.sharedViewmodel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.merge

fun NavGraphBuilder.audioEditorRoute(controller: NavController) =
    animatedComposable<PlayerSubGraph.AudioEditorRoute> { backstackEntry ->

        val route = backstackEntry.toRoute<PlayerSubGraph.AudioEditorRoute>()

        // shared view models
        val visualsViewmodel =
            backstackEntry.sharedViewmodel<PlayerVisualizerViewmodel, PlayerVisualizerViewmodelFactory>(
                controller = controller,
                factory = { factory -> factory.create(route.audioId) },
            )

        val sharedViewmodel = backstackEntry.sharedViewmodel<PlayerMetadataViewmodel, PlayerMetadataViewmodelFactory>(
            controller = controller,
            factory = { factory -> factory.create(route.audioId) },
        )

        val editorViewModel = hiltViewModel<AudioEditorViewModel, EditorViewmodelFactory>(
            creationCallback = { factory -> factory.create(route.audioId) },
        )

        val loadState by sharedViewmodel.loadState.collectAsStateWithLifecycle()
        val compressedVisuals by visualsViewmodel.compressedVisuals.collectAsStateWithLifecycle()
        val isVisualsReady by visualsViewmodel.isVisualsReady.collectAsStateWithLifecycle()

        val isPlaying by editorViewModel.isPlayerPlaying.collectAsStateWithLifecycle()
        val trackData by editorViewModel.trackData.collectAsStateWithLifecycle()
        val clipConfig by editorViewModel.clipConfig.collectAsStateWithLifecycle()
        val transformationState by editorViewModel.transformationState.collectAsStateWithLifecycle()
        val undoRedoState by editorViewModel.undoRedoState.collectAsStateWithLifecycle()

        val totalConfigs by editorViewModel.clipConfigs.collectAsStateWithLifecycle()
        val isMediaEdited by remember(totalConfigs) {
            derivedStateOf { totalConfigs.isNotEmpty() }
        }

        val lifecycleOwner = LocalLifecycleOwner.current

        // ui events handler
        UiEventsHandler(
            eventsFlow = {
                merge(
                    sharedViewmodel.uiEvent,
                    visualsViewmodel.uiEvent,
                    editorViewModel.uiEvent,
                )
            },
        )

        LaunchedEffect(lifecycleOwner) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                editorViewModel.exportBegun.collectLatest {
                    // handle nav event
                    controller.navigate(NavRoutes.VoiceRecordings) {
                        popUpTo<NavRoutes.VoiceRecordings> {
                            inclusive = true
                        }
                    }
                }
            }
        }

        LaunchedEffect(lifecycleOwner) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                editorViewModel.clipConfigs.collectLatest {
                    // when clip configs are updated the compressed visuals also get updated
                    visualsViewmodel.updateClipConfigs(it)
                }
            }
        }

        CompositionLocalProvider(LocalSharedTransitionVisibilityScopeProvider provides this) {
            AudioEditorScreen(
                loadState = loadState,
                trackData = { trackData },
                graphData = { compressedVisuals },
                onEvent = editorViewModel::onEvent,
                isVisualsReady = isVisualsReady,
                isPlaying = isPlaying,
                clipConfig = clipConfig,
                isMediaEdited = isMediaEdited,
                undoRedoState = undoRedoState,
                transformationState = transformationState,
                onDismissScreen = {
                    if (controller.previousBackStackEntry != null) {
                        controller.popBackStack()
                    }
                },
                navigation = {
                    if (controller.previousBackStackEntry != null) {
                        IconButton(onClick = dropUnlessResumed(block = controller::popBackStack)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = stringResource(R.string.back_arrow),
                            )
                        }
                    }
                },
            )
        }
    }
