package com.eva.recorderapp.voice_recorder.domain.recorder

data class RecordEncoderAndFormat(
	val encoder: Int,
	val outputFormat: Int,
	val mimeType: String,
)