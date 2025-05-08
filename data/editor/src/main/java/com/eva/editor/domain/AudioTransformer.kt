package com.eva.editor.domain

import com.eva.editor.domain.model.AudioClipConfig
import com.eva.editor.domain.model.AudioEditAction
import com.eva.recordings.domain.models.AudioFileModel
import kotlinx.coroutines.flow.Flow

typealias AudioConfigToAction = Pair<AudioClipConfig, AudioEditAction>
typealias AudioConfigToActionList = List<AudioConfigToAction>
typealias AudioConfigsList = List<AudioClipConfig>

interface AudioTransformer {

	val progress: Flow<TransformationProgress>

	suspend fun transformAudio(
		model: AudioFileModel,
		actionsList: AudioConfigToActionList
	): Result<String>


	fun cleanUp()
}