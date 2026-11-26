package com.eva.speech_enhancements.model_manager

interface ISpeechModelDownloader {
    suspend fun download(onProgress: (Int) -> Unit): Result<Unit>
}
