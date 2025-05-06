package com.eva.player.domain

import com.eva.recordings.domain.models.AudioFileModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AudioVisualizer {

	val isVisualReady: StateFlow<Boolean>

	val normalizedVisualization: Flow<FloatArray>

	suspend fun prepareVisualization(model: AudioFileModel, timePerPointInMs: Int = 10)
			: Result<Unit>

	suspend fun prepareVisualization(fileUri: String, timePerPointInMs: Int): Result<Unit>

	suspend fun prepareVisualization(fileId: Long, timePerPointInMs: Int): Result<Unit>

	fun cleanUp()
}