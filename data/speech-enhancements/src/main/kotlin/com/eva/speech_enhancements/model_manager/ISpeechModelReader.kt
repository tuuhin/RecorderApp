package com.eva.speech_enhancements.model_manager

interface ISpeechModelReader {

    suspend fun isDownloaded(): Boolean
    suspend fun delete(): Any
}
