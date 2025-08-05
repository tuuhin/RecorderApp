package com.eva.recorder.domain

import com.eva.recorder.domain.models.RecordedPoint
import com.eva.recorder.domain.models.RecorderState
import com.eva.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalTime

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
	val dataPoints: Flow<List<RecordedPoint>>

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
	suspend fun stopRecording(): Resource<Long?, Exception>

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