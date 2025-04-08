package com.eva.recorder.domain.models

data class RecordEncoderAndFormat(
	val encoder: Int,
	val outputFormat: Int,
	val mimeType: String,
)