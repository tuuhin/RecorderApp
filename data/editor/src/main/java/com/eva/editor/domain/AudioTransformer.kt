package com.eva.editor.domain

import com.eva.editor.domain.model.AudioClipConfig
import com.eva.editor.domain.model.AudioEditAction
import com.eva.recordings.domain.models.AudioFileModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

typealias AudioConfigToAction = Pair<AudioClipConfig, AudioEditAction>
typealias AudioConfigToActionList = List<AudioConfigToAction>
typealias AudioConfigsList = List<AudioClipConfig>

interface AudioTransformer {

	val transformationProgress: Flow<TransformationProgress>

	val isTransformationRunning: StateFlow<Boolean>

	suspend fun transformAudio(model: AudioFileModel, actions: AudioConfigToActionList)
			: Result<File>

	suspend fun removeTransformsFile(uri: String): Result<Boolean>

	fun cleanUp()
}