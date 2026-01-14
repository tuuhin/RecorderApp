package com.eva.recorder.domain.recorder

import com.eva.recorder.domain.models.RecorderState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalTime

interface VoiceRecorder {

	/**
	 * Current state of the recorder, determines if the recorder is playing,
	 * paused or others
	 * @see RecorderState
	 */
	val recorderState: StateFlow<RecorderState>


	/**
	 * A flow determining how long the recording has been started
	 */
	val recorderTimer: StateFlow<LocalTime>

	/**
	 * Start recording
	 */
	suspend fun startRecording()

	/**
	 * Stop the running recording
	 */
	suspend fun stopRecording(): Result<Long>

	/**
	 * Pause the ongoing recording
	 */
	suspend fun pauseRecording()

	/**
	 * Starts the paused recording
	 */
	suspend fun resumeRecording()

	/* Cancels the current running  recording
	 */
	suspend fun cancelRecording()

	/**
	 * Clears all the native allocation and other, should be called when you are done
	 * with the recorder
	 */
	fun releaseResources()

}