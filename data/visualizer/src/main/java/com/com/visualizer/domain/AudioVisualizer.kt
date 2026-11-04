package com.com.visualizer.domain

import androidx.lifecycle.LifecycleOwner
import com.eva.recordings.domain.models.AudioFileModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface AudioVisualizer {

    fun observeVisuals(owner: VisualizerSession): Flow<VisualizationData>

    fun visualizerState(owner: VisualizerSession): Flow<VisualizerState> =
        observeVisuals(owner).map { it.state }.distinctUntilChanged()

    fun normalizedVisualization(owner: VisualizerSession): Flow<FloatArray> =
        observeVisuals(owner).map { it.values }.distinctUntilChanged()

    suspend fun prepareVisualization(
        model: AudioFileModel,
        lifecycleOwner: LifecycleOwner,
        session: VisualizerSession,
        timePerPointInMs: Int = 10,
    ): Result<Unit>

    suspend fun prepareVisualization(
        fileUri: String,
        lifecycleOwner: LifecycleOwner,
        session: VisualizerSession,
        timePerPointInMs: Int,
    ): Result<Unit>

    fun cleanUp(owner: VisualizerSession)

    suspend fun cleanUpAndAwait(owner: VisualizerSession)
}
