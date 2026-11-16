package com.eva.recorder.domain

import java.io.File
import kotlinx.coroutines.flow.StateFlow

/**
 * A generic audio recorder that can be used for both android media apis and also custom apis
 */
internal interface GenericVoiceRecorder {

    /** Whether the recorder is prepared and ready to capture audio. */
    val isRecorderReady: StateFlow<Boolean>

    /** Prepares the recorder and starts capturing audio, invoking [onReady] when preparation completes. */
    suspend fun prepareAndStart(onReady: suspend () -> Unit): Result<Unit>

    /** Stops capturing audio and returns the recorded file. */
    suspend fun stopRecording(): Result<File>

    /** Pauses the active recording. */
    suspend fun pauseRecording()

    /** Resumes a paused recording. */
    suspend fun resumeRecording()

    /** Cancels the active recording without completing it. And also deletes the recording file */
    suspend fun cancelOngoingRecording()

    /** Releases the resources held by the recorder. */
    fun release()
}
