package com.eva.speech_enhancements

import com.eva.speech_enhancements.models.EnhanceOptions

interface IClearSpeechEnhancer : AutoCloseable {

    val latencySamples: Int
    val isReady: Boolean

    suspend fun prepare()

    suspend fun process(chunk: FloatArray, options: EnhanceOptions = EnhanceOptions.QuietRoom): FloatArray
    suspend fun finish(options: EnhanceOptions = EnhanceOptions.QuietRoom): FloatArray

    suspend fun reset()
}
