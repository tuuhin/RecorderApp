package com.eva.feature_editor.event

import com.eva.editor.domain.TransformationProgress

data class TransformationState(
	val isTransforming: Boolean = false,
	val progress: () -> TransformationProgress = { TransformationProgress.Idle },
	val exportFileUri: String? = null,
) {
	val isExportFileReady: Boolean
		get() = exportFileUri != null
}