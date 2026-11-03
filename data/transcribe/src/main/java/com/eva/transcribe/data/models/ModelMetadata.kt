package com.eva.transcribe.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ModelMetadata(
    @SerialName("id") val modelId: String,
    @SerialName("name") val name: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("locale") val locale: String = "en-US",
    @SerialName("version") val version: String,
    @SerialName("engine") val engine: String = "vosk",
    @SerialName("size_bytes") val sizeBytes: Long = 0L,
    @SerialName("model_uri") val modelUri: String,
    @SerialName("archive_format") val archiveFormat: String = "zip",
    @SerialName("checksum_sha256") val checksum: String? = null,
    @SerialName("license") val license: String,
)
