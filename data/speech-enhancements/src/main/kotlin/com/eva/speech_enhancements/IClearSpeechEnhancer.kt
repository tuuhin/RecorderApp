package com.eva.speech_enhancements

import com.eva.speech_enhancements.models.EnhanceAudioResult
import com.eva.speech_enhancements.models.EnhanceOptions
import java.io.File

interface IClearSpeechEnhancer : AutoCloseable {

    val latencySamples: Int

    fun create(modelDir: File)

    suspend fun prepare()

    suspend fun reset(): Boolean

    suspend fun enhance(
        pcmSamples: FloatArray,
        sampleRate: Int = 48_000,
        options: EnhanceOptions = EnhanceOptions(),
    ): EnhanceAudioResult

}
