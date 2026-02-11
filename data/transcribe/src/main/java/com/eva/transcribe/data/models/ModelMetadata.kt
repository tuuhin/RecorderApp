package com.eva.transcribe.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ModelMetadata(
	@SerialName("model") val modelId: String,
	@SerialName("version") val version: String,
	@SerialName("model_uri") val modelUri: String,
	@SerialName("locale") val locale: String = "en-us",
)
