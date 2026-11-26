package com.eva.speech_enhancements.model_manager.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SpeechModelsAssets(
    val models: List<SpeechModelMetadata> = emptyList(),
)

@Serializable
internal data class SpeechModelMetadata(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("version") val version: String,
    @SerialName("base_uri") val baseUri: String,
    @SerialName("license") val license: String? = null,
    val files: List<SpeechModelFile> = emptyList(),
)

@Serializable
internal data class SpeechModelFile(
    @SerialName("name") val name: String,
    @SerialName("size_bytes") val sizeBytes: Long,
    @SerialName("checksum_sha256") val checksumSha256: String,
)
