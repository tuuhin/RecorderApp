package com.eva.editor.domain

import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

interface AudioTrimmer {

	val progress: Flow<TransformationProgress>

	fun trimAudioFile(fileUri: String, start: Duration, end: Duration)

	fun prepareTransformer()

	fun cancelTransformation()

	fun cleanUp()
}