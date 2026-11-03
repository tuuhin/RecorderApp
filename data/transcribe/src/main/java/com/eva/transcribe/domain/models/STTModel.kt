package com.eva.transcribe.domain.models

data class STTModel(
    // Identity
    val modelId: String,
    val name: String,
    val displayName: String,
    val locale: String = "en-US",
    // Metadata
    val engine: String = "vosk",
    val downloadURI: String = "some.uri",
    val downloadedVersion: String = "0.0.0",
    val archiveSize: Long = 0L,
    val archiveFormat: String = "zip",
    val checksum: String? = null,
    val license: String? = null,
    // Local installation state
    val state: STTModelState = STTModelState.UN_AVAILABLE,
    val localModelURI: String? = null,
)
