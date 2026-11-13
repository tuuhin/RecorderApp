package com.eva.recorder.domain

import java.io.File
import kotlinx.coroutines.flow.StateFlow

internal interface GenericVoiceRecorder {

    val isRecorderReady: StateFlow<Boolean>

    suspend fun prepareAndStart(onReady: suspend () -> Unit): Result<Unit>

    suspend fun stopRecording(): Result<File>
    suspend fun discardRecordingFile(file: File)
    suspend fun pauseRecording(): Result<Unit>
    suspend fun resumeRecording()
    suspend fun cancelOngoingRecording()
    fun release()
}
