package com.eva.editor.domain

import com.eva.editor.data.AudioClipConfig
import com.eva.recordings.domain.models.AudioFileModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface AudioTrimmer {

	val progress: Flow<TransformationProgress>

	val errorsFlow: SharedFlow<Exception>

	fun trimAudioFile(model: AudioFileModel, clipConfig: AudioClipConfig): Result<Unit>

	fun cancelTransformation()

	fun cleanUp()
}