package com.eva.transcribe.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ModelsMetadataList(
	@SerialName("models")
	val models: List<ModelMetadata> = emptyList()
)
