package com.eva.editor.domain

sealed interface TransformationProgress {
	data class Progress(val amount: Int) : TransformationProgress
	data object Waiting : TransformationProgress
	data object Idle : TransformationProgress
	data object UnAvailable : TransformationProgress
}