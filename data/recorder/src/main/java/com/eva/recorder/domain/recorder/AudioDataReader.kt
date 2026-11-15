package com.eva.recorder.domain.recorder

import com.eva.recorder.domain.models.RecorderState
import kotlinx.coroutines.flow.Flow

internal interface AudioDataReader {

    fun initiateRecorder()
    suspend fun startRecorder()
    suspend fun stopRecorder()
    fun releaseRecorder()

    fun readRecorderRawBytes(state: RecorderState): Flow<Pair<ShortArray, Int>>
}
