package com.eva.recorderapp.voice_recorder.data.recorder

import android.Manifest
import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.domain.emums.RecorderState
import com.eva.recorderapp.voice_recorder.domain.recorder.RecorderFileProvider
import com.eva.recorderapp.voice_recorder.domain.recorder.RecorderStopWatch
import com.eva.recorderapp.voice_recorder.domain.recorder.VoiceRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.datetime.LocalTime
import java.io.IOException

private const val LOGGER_TAG = "VOICE_RECORDER"
private const val SAMPLING_SIZE = 100

class VoiceRecorderImpl(
	private val context: Context,
	private val fileProvider: RecorderFileProvider,
	private val stopWatch: RecorderStopWatch,
) : VoiceRecorder {

	// recorder related
	private var _recorder: MediaRecorder? = null
	private var _bufferReader: BufferedAmplitudeReader? = null

	// recodings file related
	private var _fd: ParcelFileDescriptor? = null
	private var _currentRecordingUri: Uri? = null

	// locks ensures a operation complete before an other operation can start
	val operationLock = Mutex(false)

	val _hasRecordPremission: Boolean
		get() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
				PermissionChecker.PERMISSION_GRANTED

	override val recorderState: StateFlow<RecorderState>
		get() = stopWatch.recorderState

	override val recorderTimer: StateFlow<LocalTime>
		get() = stopWatch.elapsedTime

	@OptIn(ExperimentalCoroutinesApi::class)
	override val maxAmplitudes: Flow<FloatArray>
		get() = recorderState
			.flatMapLatest { state ->
				_bufferReader?.readAmplitudeBuffered(state)
					?: emptyFlow()
			}

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

		_bufferReader = BufferedAmplitudeReader(
			recorder = _recorder,
			maxBufferSize = SAMPLING_SIZE
		)
		Log.d(LOGGER_TAG, "CREATED RECORDER AND AMPLITUDE SUCCESSFULLY")
	}

	/**
	 * Creates the file uri in which the audio to be recorded and initiate
	 * the recorder parameters
	 */
	private suspend fun initiateRecorderParams() = coroutineScope {
		if (_recorder == null) createRecorder()

		// ensures the file is being created in a differnt coroutine
		val file = async(Dispatchers.IO) { fileProvider.createUriForRecording() }
		_currentRecordingUri = file.await()

		if (_currentRecordingUri == null) {
			Log.i(LOGGER_TAG, "CANNOT CREATE FILE FOR RECORDING")
			return@coroutineScope
		}

		Log.d(LOGGER_TAG, "NEW_FILE_URI_CREATED")

		_fd = context.contentResolver.openFileDescriptor(_currentRecordingUri!!, "w")
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
		_currentRecordingUri?.let { uri ->
			fileProvider.updateUriAfterRecording(uri)
			Log.d(LOGGER_TAG, "RECORDER FILE UPDATED")
		}
		// set recording uri to null and close the socket
		_currentRecordingUri = null
		// resets the recorder for  next recording
		Log.d(LOGGER_TAG, "RESETING THE RECORDER")
		_recorder?.reset()
	}

	private suspend fun stopAndDeleteFileMetaData() {
		// close the descriptor
		_fd?.close()
		_fd = null
		Log.d(LOGGER_TAG, "FILE DESCRIPTOR CLOSED")
		// update the file
		_currentRecordingUri?.let { uri ->
			fileProvider.deleteUriIfNotPending(uri)
			Log.d(LOGGER_TAG, "RECORDER FILE DELETED")
		}
		// set recording uri to null and close the socket
		_currentRecordingUri = null
		// resets the recorder for  next recording
		Log.d(LOGGER_TAG, "RESETING THE RECORDER")
		_recorder?.reset()
	}

	override suspend fun startRecording() {
		// if its holding the lock dont do anything
		if (operationLock.holdsLock(this)) {
			Log.d(LOGGER_TAG, "CANNOT START RECORDING ITS LOCKED")
			return
		}
		// staring an operation lock it
		operationLock.lock(this)
		// prepare the recording params
		if (_currentRecordingUri == null) initiateRecorderParams()
		try {
			Log.i(LOGGER_TAG, "PREPARING RECORDER")
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
		} finally {
			// unlocks the current lock
			operationLock.unlock(this)
			Log.d(LOGGER_TAG, "CLEARING LOCK IN START")
		}
	}

	override suspend fun stopRecording() {
		// if its holding the lock dont do anything
		if (operationLock.holdsLock(this)) {
			Log.d(LOGGER_TAG, "CANNOT STOP RECORDING ITS LOCKED")
			return
		}
		// staring an operation lock it
		operationLock.lock(this)
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
		} finally {
			// unlocks the current lock
			operationLock.unlock(this)
			Log.d(LOGGER_TAG, "CLEARING LOCK IN STOP")
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

	override suspend fun cancelRecording() {
		// if its holding the lock dont do anything
		if (operationLock.holdsLock(this)) {
			Log.d(LOGGER_TAG, "CANNOT CANCEL RECORDING ITS LOCKED")
			return
		}
		// staring an operation lock it
		operationLock.lock(this)
		try {
			// cancel the timer watch
			Log.d(LOGGER_TAG, "STOPWATCH STOPPED")
			stopWatch.cancel()
			//stop the ongoing recording
			_recorder?.stop()
			// delete the current recording
			stopAndDeleteFileMetaData()
			Log.d(LOGGER_TAG, "RECORDER STOPPED")
		} catch (e: Exception) {
			e.printStackTrace()
		} finally {
			// unlocks the current lock
			operationLock.unlock(this)
			Log.d(LOGGER_TAG, "CLEARING LOCK IN CANCEL")
		}
	}

	override fun releaseResources() {
		// closing the descriptor
		Log.d(LOGGER_TAG, "CLOSING THE FILE DESCRIPTOR")
		_fd?.close()
		_fd = null
		//set recorder null
		_currentRecordingUri == null
		// deleting the file
		_currentRecordingUri?.let { uri ->
			// run blocking as we want to run this blocking code in the IO thread.
			runBlocking(Dispatchers.IO) {
				Log.d(LOGGER_TAG, "CLEARING THE FILE AS RECORDER CLEAR METHOD IS CALLED")
				fileProvider.deleteUriIfNotPending(uri)
			}
			Toast.makeText(context, R.string.delete_recording_uri, Toast.LENGTH_SHORT).show()
		}
		//set buffer reader to null
		_bufferReader = null
		Log.d(LOGGER_TAG, "CLEARING THE BUFFER READER")
		// clear the recorder resources
		Log.d(LOGGER_TAG, "RELEASE RECORDER")
		_recorder?.release()
		_recorder == null
		// resetting the stopwatch
		Log.d(LOGGER_TAG, "RESETTING STOPWATCH")
		stopWatch.reset()

	}

}