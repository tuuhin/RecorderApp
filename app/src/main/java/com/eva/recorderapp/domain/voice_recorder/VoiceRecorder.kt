package com.eva.recorderapp.domain.voice_recorder

interface VoiceRecorder {

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