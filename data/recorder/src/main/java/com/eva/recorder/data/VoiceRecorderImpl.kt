package com.eva.recorder.data

import android.Manifest
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.eva.datastore.domain.enums.RecordQuality
import com.eva.datastore.domain.repository.RecorderAudioSettingsRepo
import com.eva.location.domain.repository.LocationProvider
import com.eva.recorder.data.reader.BufferedAmplitudeReader
import com.eva.recorder.domain.VoiceRecorder
import com.eva.recorder.domain.exceptions.RecorderNotConfiguredException
import com.eva.recorder.domain.models.RecordEncoderAndFormat
import com.eva.recorder.domain.models.RecorderState
import com.eva.recorder.domain.stopwatch.RecorderStopWatch
import com.eva.recorder.utils.DurationToAmplitudeList
import com.eva.recordings.domain.provider.RecorderFileProvider
import com.eva.utils.RecorderConstants
import com.eva.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalTime
import java.io.File
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

private const val LOGGER_TAG = "VOICE_RECORDER"

internal class VoiceRecorderImpl(
	private val context: Context,
	private val fileProvider: RecorderFileProvider,
	private val settings: RecorderAudioSettingsRepo,
	private val locationProvider: LocationProvider,
) : VoiceRecorder {

	private val stopWatch = RecorderStopWatch(delayTime = 50.milliseconds)

	// recording format and encoder
	private val format: RecordEncoderAndFormat
		get() = settings.audioSettings.encoders.recordFormat

	private val channelCount: Int
		get() = if (settings.audioSettings.enableStereo) 2 else 1

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
	override val dataPoints: Flow<DurationToAmplitudeList>
		get() = recorderState
			.flatMapLatest { state -> _bufferReader?.readAmplitudeBuffered(state) ?: emptyFlow() }
			.map { points ->
				points.map { (millis, amp) -> millis.milliseconds to amp }
			}
			.flowOn(Dispatchers.Default)

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

		_bufferReader = BufferedAmplitudeReader(
			recorder = _recorder,
			stopWatch = stopWatch,
			delayRate = RecorderConstants.AMPS_READ_DELAY_RATE,
			bufferSize = RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE
		)
		Log.d(LOGGER_TAG, "CREATED RECORDER AND AMPLITUDE SUCCESSFULLY")
	}

	/**
	 * Creates the file uri in which the audio to be recorded and initiate
	 * the recorder parameters
	 */
	@OptIn(ExperimentalCoroutinesApi::class)
	private suspend fun initiateRecorderParams() = coroutineScope {
		if (_recorder == null) createRecorder()

		// ensures the file is being created in a different coroutine
		val fileDeferred = async(Dispatchers.IO) {
			fileProvider.createFileForRecording(format.fileExtension)
		}

		// location deferred for current location if available
		val locationDeferred = async(Dispatchers.IO) {
			if (settings.audioSettings.addLocationInfoInRecording)
				return@async locationProvider.invoke()
			null
		}

		Log.d(LOGGER_TAG, "CONCURRENTLY FETCHING LOCATION AND PREPARING FILE")
		awaitAll(locationDeferred, fileDeferred)

		_recordingFile = fileDeferred.getCompleted()

		val locationResult = locationDeferred.getCompleted()
		val locationSuccess = (locationResult as? Resource.Success)
		val locationFailed = (locationResult as? Resource.Error)

		if (locationFailed != null) {
			Log.w(LOGGER_TAG, "${locationFailed.message ?: locationFailed.error.message}")
		}

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
			//set location can only add location to mp4 and 3gp files
			locationSuccess?.data?.let {
				Log.d(LOGGER_TAG, "LOCATION ADDED :$it")
				setLocation(
					it.latitude.toFloat(),
					it.longitude.toFloat()
				)
			}
		}
		// metrics logs
		_recorder?.metrics?.let { bundle ->
			val bitrate = bundle.getInt(MediaRecorder.MetricsConstants.AUDIO_BITRATE)
			val sampleRte = bundle.getInt(MediaRecorder.MetricsConstants.AUDIO_SAMPLERATE)
			val channel = bundle.getInt(MediaRecorder.MetricsConstants.AUDIO_CHANNELS)
			Log.i(LOGGER_TAG, "RECORDER METRICS AFTER CONFIGURATION")
			Log.i(LOGGER_TAG, "SAMPLING RATE : $sampleRte")
			Log.i(LOGGER_TAG, "ENCODING BIT RATE : $bitrate")
			Log.i(LOGGER_TAG, "CHANNEL COUNT :$channel")
		}
	}

	/**
	 * Method to be called when recording has been finished, and you update the file
	 * metadata
	 */
	private suspend fun updateRecordingToExternalStorage(): Resource<Long?, Exception> {
		// update the file
		val recordingId = _recordingFile?.let { file ->
			withContext(Dispatchers.IO) {
				Log.d(LOGGER_TAG, "RECORDER FILE UPDATED")
				fileProvider.transferFileDataToStorage(file = file, mimeType = format.mimeType)
			}
		} ?: return Resource.Error(RecorderNotConfiguredException())
		// set recording uri to null and close the socket
		_recordingFile = null
		// resets the recorder for  next recording
		Log.d(LOGGER_TAG, "RESTING THE RECORDER")
		_recorder?.reset()
		return Resource.Success(recordingId)
	}

	private suspend fun stopAndDeleteFileMetaData() {
		// update the file
		_recordingFile?.let { file ->
			// non-cancellable as file should be deleted
			withContext(NonCancellable + Dispatchers.IO) {
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
		// current uri is already set cannot set it again
		if (_recordingFile != null) {
			Log.d(LOGGER_TAG, "CURRENT URI IS ALREADY SET")
			return
		}
		// staring an operation lock it
		operationLock.lock(this)
		try {
			// prepare the recording params
			stopWatch.prepare()
			Log.i(LOGGER_TAG, "PREPARING FILE FOR RECORDING")
			initiateRecorderParams()
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

	override suspend fun stopRecording(): Resource<Long?, Exception> {
		// if it's holding the lock don't do anything
		if (operationLock.holdsLock(this)) {
			Log.d(LOGGER_TAG, "CANNOT STOP RECORDING ITS LOCKED")
			// returning null as there was no error but a lock
			return Resource.Success(null)
		}
		if (_recordingFile == null) {
			Log.d(LOGGER_TAG, "FILE URI IS NOT SET SO RECORDER IS NOT READY")
		}
		// staring an operation lock it
		operationLock.lock(this)
		return try {
			// reset the timer
			Log.d(LOGGER_TAG, "STOPWATCH STOPPED")
			stopWatch.stop()
			//stop the ongoing recording
			_recorder?.stop()
			Log.d(LOGGER_TAG, "RECORDER STOPPED")
			// update the file
			updateRecordingToExternalStorage()
		} catch (e: IllegalStateException) {
			e.printStackTrace()
			Resource.Error(e, message = "Cannot stop as start wasn't called")
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