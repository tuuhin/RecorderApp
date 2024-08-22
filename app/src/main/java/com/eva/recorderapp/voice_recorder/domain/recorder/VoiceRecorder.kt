package com.eva.recorderapp.voice_recorder.domain.recorder

import com.eva.recorderapp.voice_recorder.domain.recorder.emums.RecorderState
import kotlinx.coroutines.flow.Flow
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
	 * A series of amplitudes of the current sampled audio record
	 */
	val maxAmplitudes: Flow<FloatArray>

	/**
	 * A flow determining how long the recording has been started
	 */
	val recorderTimer: StateFlow<LocalTime>

	/**
	 * Initiates the recorder to be used
	 */
	fun createRecorder()

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

	/* Cancels the current running  recording
	 */
	suspend fun cancelRecording()

	/**
	 * Clean up funtion to clean all the allocated resources with the recorder
	 */
	fun releaseResources()

	companion object {
		const val RECORDER_AMPLITUDES_BUFFER_SIZE = 120
	}
}