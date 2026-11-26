package com.eva.speech_enhancements.model_manager

import java.io.File

interface SpeechModelProvider {

    val isDownloaded: Boolean
    val modelPath: Result<File>

    suspend fun downloadModel(): Result<Unit>
    suspend fun resetModel()
}
