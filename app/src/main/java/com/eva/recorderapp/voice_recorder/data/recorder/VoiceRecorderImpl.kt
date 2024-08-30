package com.eva.recorderapp.voice_recorder.data.recorder

import android.Manifest
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.domain.datastore.enums.RecordQuality
import com.eva.recorderapp.voice_recorder.domain.datastore.repository.RecorderAudioSettingsRepo
import com.eva.recorderapp.voice_recorder.domain.recorder.RecorderFileProvider
import com.eva.recorderapp.voice_recorder.domain.recorder.RecorderStopWatch
import com.eva.recorderapp.voice_recorder.domain.recorder.VoiceRecorder
import com.eva.recorderapp.voice_recorder.domain.recorder.emums.RecorderState
import com.eva.recorderapp.voice_recorder.domain.recorder.models.RecordEncoderAndFormat
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
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalTime
import java.io.File
import java.io.IOException

private const val LOGGER_TAG = "VOICE_RECORDER"

class VoiceRecorderImpl(
	private val context: Context,
	private val fileProvider: RecorderFileProvider,
	private val stopWatch: RecorderStopWatch,
	private val settings: RecorderAudioSettingsRepo,
) : VoiceRecorder {

	// recording format and encoder
	private val format: RecordEncoderAndFormat
		get() = settings.audioSettings.encoders.recordFormat

	private val channelCount: Int
		get() = if (settings.audioSettings.enableStero) 2 else 1

	// recorder quality
	private val quality: RecordQuality
		get() = settings.audioSettings.quality

	// recorder related
	private var _recorder: MediaRecorder? = null
	private var _bufferReader: BufferedAmplitudeReader? = null

	// recordings file related
	private var _recordingFile: File? = null

	// locks ensures an operation complete before another operation can start
	private val operationLock = Mutex(false)

	private val _hasRecordPermission: Boolean
		get() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
				PermissionChecker.PERMISSION_GRANTED

	override val recorderState: StateFlow<RecorderState>
		get() = stopWatch.recorderState

	override val recorderTimer: StateFlow<LocalTime>
		get() = stopWatch.elapsedTime

	@OptIn(ExperimentalCoroutinesApi::class)
	override val maxAmplitudes: Flow<FloatArray>
		get() = recorderState.flatMapLatest { state ->
			_bufferReader?.readAmplitudeBuffered(state)
				?: emptyFlow()
		}

	private val _errorListener = MediaRecorder.OnErrorListener { _, _, _ ->
		Log.e(LOGGER_TAG, "SOME ERROR OCCURRED WITH RECORDER")
	}

	@Suppress("DEPRECATION")
	override fun createRecorder() {
		// no perms granted
		if (!_hasRecordPermission) {
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

		_recorder?.setOnErrorListener(_errorListener)

		_bufferReader = BufferedAmplitudeReader(recorder = _recorder)
		Log.d(LOGGER_TAG, "CREATED RECORDER AND AMPLITUDE SUCCESSFULLY")
	}

	/**
	 * Creates the file uri in which the audio to be recorded and initiate
	 * the recorder parameters
	 */
	private suspend fun initiateRecorderParams(): Boolean {
		return coroutineScope {
			if (_recorder == null) createRecorder()

			// ensures the file is being created in a different coroutine
			val file = async(Dispatchers.IO) {
				fileProvider.createFileForRecoring(format.fileExtension)
			}

			_recordingFile = file.await()

			Log.d(LOGGER_TAG, "NEW_FILE_URI_CREATED")

			_recorder?.apply {
				setOutputFile(_recordingFile)
				setAudioSource(MediaRecorder.AudioSource.MIC)
				// recorder format
				setOutputFormat(format.outputFormat)
				// formater channel and sampling
				setAudioEncoder(format.encoder)
				setAudioChannels(channelCount)
				// recording quality
				setAudioSamplingRate(quality.sampleRate)
				setAudioEncodingBitRate(quality.bitRate)
			}
			_recorder?.metrics?.let { bundle ->
				val bitrate = bundle.getInt(MediaRecorder.MetricsConstants.AUDIO_BITRATE)
				val sampleRte = bundle.getInt(MediaRecorder.MetricsConstants.AUDIO_SAMPLERATE)
				val channel = bundle.getInt(MediaRecorder.MetricsConstants.AUDIO_CHANNELS)
				Log.i(LOGGER_TAG, "RECORDER METRICS AFTER CONFIGURATION")
				Log.i(LOGGER_TAG, "SAMPLING RATE : $sampleRte")
				Log.i(LOGGER_TAG, "ENCODING BIT RATE : $bitrate")
				Log.i(LOGGER_TAG, "CHANNEL COUNT :$channel")
			}
			true
		}
	}

	/**
	 * Method to be called when recording has been finished, and you update the file
	 * metadata
	 */
	private suspend fun updateFileDataToExternalStorage() {
		// update the file
		_recordingFile?.let { file ->
			withContext(Dispatchers.IO) {
				fileProvider.transferFileDataToStorage(file, format)
				Log.d(LOGGER_TAG, "RECORDER FILE UPDATED")
			}
		}
		// set recording uri to null and close the socket
		_recordingFile = null
		// resets the recorder for  next recording
		Log.d(LOGGER_TAG, "RESTING THE RECORDER")
		_recorder?.reset()
	}

	private suspend fun stopAndDeleteFileMetaData() {
		// update the file
		_recordingFile?.let { file ->
			withContext(Dispatchers.IO) {
				fileProvider.deleteCreatedFile(file)
				Log.d(LOGGER_TAG, "RECORDER FILE DELETED")
			}
		}
		// set recording uri to null and close the socket
		_recordingFile = null
		// resets the recorder for  next recording
		Log.d(LOGGER_TAG, "RESTING THE RECORDER")
		_recorder?.reset()
	}

	override suspend fun startRecording() {
		// if it's holding the lock don't do anything
		if (operationLock.holdsLock(this)) {
			Log.d(LOGGER_TAG, "CANNOT START RECORDING ITS LOCKED")
			return
		}
		// staring an operation lock it
		operationLock.lock(this)
		// current uri is already set cannot set it again
		if (_recordingFile != null) {
			Log.d(LOGGER_TAG, "CURRENT URI IS ALREADY SET")
			return
		}
		try {
			// prepare the recording params
			stopWatch.prepare()
			Log.i(LOGGER_TAG, "PREPARING FILE FOR RECORDING")
			val isOK = initiateRecorderParams()
			if (!isOK) {
				Log.d(LOGGER_TAG, "CANNOT INITIATE RECORDER PARAMS")
				val message = context.getString(R.string.cannot_create_file)
				Toast.makeText(context, message, Toast.LENGTH_SHORT)
					.show()
				return
			}
			// prepare the recorder
			_recorder?.prepare()
			Log.d(LOGGER_TAG, "RECORDER PREPARED")
			//start the recorder
			_recorder?.start()
			stopWatch.startOrResume()
			Log.d(LOGGER_TAG, "RECORDER STARTED")
		} catch (e: IOException) {
			e.printStackTrace()
		} finally {
			// unlocks the current lock
			operationLock.unlock(this)
			Log.d(LOGGER_TAG, "CLEARING LOCK IN START")
		}
	}

	override suspend fun stopRecording() {
		// if it's holding the lock don't do anything
		if (operationLock.holdsLock(this)) {
			Log.d(LOGGER_TAG, "CANNOT STOP RECORDING ITS LOCKED")
			return
		}
		if (_recordingFile == null) {
			Log.d(LOGGER_TAG, "FILE URI IS NOT SET SO RECORDER IS NOT READY")
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
			updateFileDataToExternalStorage()
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
		// if it's holding the lock don't do anything
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
			Log.d(LOGGER_TAG, "RECORDER STOPPED")
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
		// delete the recording file if its exits
		_recordingFile?.let { file ->
			// run blocking as we want to run this blocking code in the IO thread.
			runBlocking {
				Log.d(LOGGER_TAG, "CLEARING THE FILE AS RECORDER CLEAR METHOD IS CALLED")
				fileProvider.deleteCreatedFile(file)
			}
		}
		//set recording file to null
		_recordingFile = null
		//set buffer reader to null
		_bufferReader = null
		Log.d(LOGGER_TAG, "CLEARING THE BUFFER READER")
		// clear the recorder resources
		Log.d(LOGGER_TAG, "RELEASE RECORDER")
		_recorder?.release()
		_recorder = null
		// resetting the stopwatch
		Log.d(LOGGER_TAG, "RESETTING STOPWATCH")
		stopWatch.reset()

	}

}