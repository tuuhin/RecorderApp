package com.eva.recorderapp.voice_recorder.domain.recorder

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface VoiceRecorder {


	val isRecorderRunning: StateFlow<Boolean>

	val maxAmplitudes: Flow<FloatArray>

	/**
	 * Start recording
	 */
	suspend fun startRecording()

	/**
	 * Stop the running recording
	 */
	suspend fun stopRecording()

	/**
	 * Pause the ongoing recording
	 */
	fun pauseRecording()

	/**
	 * Starts the paused recording
	 */
	fun resumeRecording()

	/**
	 * Clean up funtion to clean all the allocated resources with the recorder
	 */
	suspend fun releaseResources()
}