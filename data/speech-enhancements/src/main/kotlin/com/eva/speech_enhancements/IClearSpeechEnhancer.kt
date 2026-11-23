package com.eva.speech_enhancements

import com.eva.speech_enhancements.models.EnhanceOptions
import com.eva.speech_enhancements.models.EnhanceAudioResult

interface IClearSpeechEnhancer : AutoCloseable {

    val latencySamples: Int

    fun create(modelDir: String)

    suspend fun prepare()

    suspend fun enhance(
        pcmSamples: FloatArray,
        sampleRate: Int = 48_000,
        options: EnhanceOptions = EnhanceOptions(),
    ): EnhanceAudioResult

}
