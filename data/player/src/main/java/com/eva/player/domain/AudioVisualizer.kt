package com.eva.player.domain

import com.eva.recordings.domain.models.AudioFileModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AudioVisualizer {

	val isVisualReady: StateFlow<Boolean>

	val visualization: Flow<FloatArray>

	fun compressedVisualisation(length: Int): Flow<FloatArray>

	suspend fun prepareVisualization(model: AudioFileModel, timePerPointInMs: Int = 10)
			: Result<Unit>

	fun cleanUp()
}