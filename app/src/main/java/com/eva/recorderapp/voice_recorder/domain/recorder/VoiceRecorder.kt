package com.eva.recorderapp.voice_recorder.domain.recorder

import com.eva.recorderapp.voice_recorder.domain.recorder.emums.RecorderState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalTime
import kotlin.time.Duration.Companion.milliseconds

typealias MicrophoneDataPoint = Pair<Long, Float>

interface VoiceRecorder {

	/**
	 * Current state of the recorder, determines if the recorder is playing,
	 * paused or others
	 * @see RecorderState
	 */
	val recorderState: StateFlow<RecorderState>

	/**
	 * A series of data-points for the current recording.
	 */
	val dataPoints: Flow<List<MicrophoneDataPoint>>

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
	 * Clears all the native allocation and other, should be called when you are done
	 * with the recorder
	 */
	fun releaseResources()

	companion object {
		// don't change the values
		const val RECORDER_AMPLITUDES_BUFFER_SIZE = 100
		val AMPS_READ_DELAY_RATE = 100.milliseconds
	}
}