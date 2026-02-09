package com.eva.transcribe.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TranscriptionResult(
	@SerialName("text") val fullResult: String = "",
	@SerialName("partial") val partial: String = ""
)