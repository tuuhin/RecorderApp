package com.eva.recorderapp.voice_recorder.data.recorder

import android.Manifest
import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.eva.recorderapp.voice_recorder.domain.emums.RecorderState
import com.eva.recorderapp.voice_recorder.domain.recorder.RecorderFileProvider
import com.eva.recorderapp.voice_recorder.domain.recorder.RecorderStopWatch
import com.eva.recorderapp.voice_recorder.domain.recorder.VoiceRecorder
import com.eva.recorderapp.voice_recorder.domain.util.flowToFixedSizeCollection
import com.eva.recorderapp.voice_recorder.domain.util.toNormalizedValues
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalTime
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

private const val LOGGER_TAG = "VOICE_RECORDER"

class VoiceRecorderImpl(
	private val context: Context,
	private val fileProvider: RecorderFileProvider,
	private val stopWatch: RecorderStopWatch,
) : VoiceRecorder {

	private var _recorder: MediaRecorder? = null
	private var _recordingUri: Uri? = null
	private var _fd: ParcelFileDescriptor? = null

	val _hasRecordPremission: Boolean
		get() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
				PermissionChecker.PERMISSION_GRANTED

	private val _isAudioSourceConfigured: Boolean?
		get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			// not null ensures its set
			_recorder?.activeRecordingConfiguration?.audioSource != null
		} else null

	override val recorderState: StateFlow<RecorderState>
		get() = stopWatch.recorderState

	override val recorderTimer: StateFlow<LocalTime>
		get() = stopWatch.elapsedTime

	@OptIn(ExperimentalCoroutinesApi::class)
	override val maxAmplitudes: Flow<FloatArray>
		get() = recorderState
			.flatMapLatest(::readSampleAmplitude)
			.flowToFixedSizeCollection(100)
			.toNormalizedValues()

	private fun readSampleAmplitude(state: RecorderState): Flow<Int> = flow {
		try {
			while (_recorder != null && state == RecorderState.RECORDING) {
				// check if audio source set
				if (_isAudioSourceConfigured == false) {
					Log.i(LOGGER_TAG, "AUDIO SOURCE NOT CONFIGURED")
					break
				}
				delay(20.milliseconds)
				// record the max amplitude of the sample
				val amplitude = _recorder?.maxAmplitude ?: 0
				emit(amplitude)
			}
		} catch (e: CancellationException) {
			// if the child flow is canceled while suspending in delay method
			// throw cancelation exception
			throw e
		} catch (e: IllegalStateException) {
			Log.wtf(LOGGER_TAG, "AUDIO SOURCE NOT SET", e)
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}.flowOn(Dispatchers.IO)


	@Suppress("DEPRECATION")
	override fun createRecorder() {
		// no perms granted
		if (!_hasRecordPremission) {
			Log.i(LOGGER_TAG, "NO RECORD PERMISSION FOUND")
			return
		}
		// recorder already set
		if (_recorder != null) {
			Log.i(LOGGER_TAG, "RECORDER ALREADY INITIATED")
			return
		}
		// set recorder
		_recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
			MediaRecorder(context)
		else MediaRecorder()

		Log.d(LOGGER_TAG, "CREATED RECORDER SUCCESSFULLY")
	}

	/**
	 * Creates the file uri in which the audio to be recorded and initiate
	 * the recorder parameters
	 */
	private suspend fun initiateRecorderParams() = coroutineScope {
		if (_recorder == null) createRecorder()

		val file = async(Dispatchers.IO) { fileProvider.createFileForRecording() }
		// ensures the file is being created in a differnt coroutine
		_recordingUri = file.await()

		Log.d(LOGGER_TAG, "NEW_FILE_URI_CREATED")

		_fd = context.contentResolver.openFileDescriptor(_recordingUri!!, "w")
		Log.d(LOGGER_TAG, "FILE DESCRIPTOR SET")

		val fileDescriptor = _fd?.fileDescriptor ?: return@coroutineScope

		_recorder?.apply {
			setAudioSource(MediaRecorder.AudioSource.MIC)
			setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
			setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
			setOutputFile(fileDescriptor)
		}
		Log.d(LOGGER_TAG, "RECORDER CONFIGURED")
	}

	/**
	 * Method to be called when recording has been finished and you update the file
	 * metadata
	 */
	private suspend fun stopAndUpdateFileMetaData() {
		// close the descriptor
		_fd?.close()
		_fd = null
		Log.d(LOGGER_TAG, "FILE DESCRIPTOR CLOSED")
		// update the file
		_recordingUri?.let { uri ->
			fileProvider.updateFileAfterRecording(uri)
			Log.d(LOGGER_TAG, "RECORDER FILE UPDATED")
		}
		// set recording uri to null and close the socket
		_recordingUri = null
		// resets the recorder for  next recording
		Log.d(LOGGER_TAG, "RESETING THE RECORDER")
		_recorder?.reset()
	}

	override suspend fun startRecording() {
		if (_recordingUri == null) initiateRecorderParams()
		try {
			// prepare the recorder
			_recorder?.prepare()
			//start the recorder
			_recorder?.start()
			Log.d(LOGGER_TAG, "RECORDER PREPARED AND STARTED")
			// set is recording to true
			stopWatch.startOrResume()
			Log.d(LOGGER_TAG, "IS RECORDING TO TRUE")
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}

	override suspend fun stopRecording() {
		try {
			// reset the timer
			Log.d(LOGGER_TAG, "STOPWATCH STOPPED")
			stopWatch.stop()
			//stop the ongoing recording
			_recorder?.stop()
			Log.d(LOGGER_TAG, "RECORDER STOPPED")
			// update the file
			stopAndUpdateFileMetaData()
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}

	override fun pauseRecording() {
		try {
			//pause recorder
			Log.d(LOGGER_TAG, "STOPWATCH PAUSED")
			stopWatch.pause()
			//pause recorder
			_recorder?.pause()
			Log.d(LOGGER_TAG, "RECORDER PAUSED")
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}

	override fun resumeRecording() {
		try {
			//resume stopwatch
			Log.d(LOGGER_TAG, "STOPWATCH RESUMED")
			stopWatch.startOrResume()
			//resume recorder
			_recorder?.resume()
			Log.d(LOGGER_TAG, "RECORDER RESUMED")
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}

	override fun releaseResources() {
		// closing the descriptor
		Log.d(LOGGER_TAG, "CLOSING THE FILE DESCRIPTOR")
		_fd?.close()
		_fd = null
		// deleting the file
		_recordingUri?.let { uri ->
			// clear the file if not save correctlty
			runBlocking(Dispatchers.IO) {
				Log.d(LOGGER_TAG, "CLEARING THE FILE AS RECORDER CLEAR METHOD IS CALLED")
				fileProvider.cancelFileCreation(uri)
			}
		}
		//set recorder null
		_recordingUri == null
		// clear the recorder resources
		Log.d(LOGGER_TAG, "RELEASE RECORDER")
		_recorder?.release()
		_recorder == null
		// resetting the stopwatch
		Log.d(LOGGER_TAG, "RESETTING STOPWATCH")
		stopWatch.reset()

	}
}