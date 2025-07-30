package com.eva.recorder.domain.models


data class RecordedPoint(
	val timeInMillis: Long = 0L,
	val rmsValue: Float = 0f,
	val isPaddingPoint: Boolean = false,
)