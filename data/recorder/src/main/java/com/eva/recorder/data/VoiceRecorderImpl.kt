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
import com.eva.recorder.data.reader.AudioRecordAmplitudeReader
import com.eva.recorder.domain.VoiceRecorder
import com.eva.recorder.domain.exceptions.RecorderNotConfiguredException
import com.eva.recorder.domain.models.RecordEncoderAndFormat
import com.eva.recorder.domain.models.RecordedPoint
import com.eva.recorder.domain.models.RecorderState
import com.eva.recorder.domain.stopwatch.RecorderStopWatch
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalTime
import java.io.File
import java.io.IOException

private const val TAG = "VOICE_RECORDER"

@OptIn(ExperimentalCoroutinesApi::class)
internal class VoiceRecorderImpl(
	private val context: Context,
	private val fileProvider: RecorderFileProvider,
	private val settings: RecorderAudioSettingsRepo,
	private val locationProvider: LocationProvider,
) : VoiceRecorder {

	private val _stopWatch = RecorderStopWatch(delayTime = RecorderConstants.AMPS_READ_DELAY_RATE)

	private val _reader by lazy {
		AudioRecordAmplitudeReader(
			stopWatch = _stopWatch,
			settings = settings,
			delayRate = RecorderConstants.AMPS_READ_DELAY_RATE
		)
	}

	// recording format and encoder
	private val format: RecordEncoderAndFormat
		get() = settings.audioSettings.encoders.recordFormat

	private val channelCount: Int
		get() = if (settings.audioSettings.enableStereo) 2 else 1

	// recorder quality
	private val quality: RecordQuality
		get() = settings.audioSettings.quality

	private var _recorder: MediaRecorder? = null
	private var _recordingFile: File? = null

	// locks ensures an operation complete before another operation can start
	private val _lock = Mutex(false)

	private val _hasRecordPermission: Boolean
		get() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
				PermissionChecker.PERMISSION_GRANTED

	override val recorderState: StateFlow<RecorderState>
		get() = _stopWatch.recorderState

	override val recorderTimer: StateFlow<LocalTime>
		get() = _stopWatch.elapsedTime

	override val dataPoints: Flow<List<RecordedPoint>>
		get() = _stopWatch.recorderState
			.flatMapLatest(_reader::readAmplitudeBuffered)

	@Suppress("DEPRECATION")
	private fun createRecorder(): Boolean {
		// no perms granted
		if (!_hasRecordPermission) {
			Log.i(TAG, "NO RECORD PERMISSION FOUND")
			return false
		}
		// recorder already set
		if (_recorder != null) {
			Log.i(TAG, "RECORDER ALREADY INITIATED")
			return false
		}
		// set recorder
		_recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
			MediaRecorder(context)
		else MediaRecorder()

		_recorder?.setOnErrorListener { mr, what, extra ->
			Log.w(TAG, "SOME ERROR OCCURRED :$what")
		}

		Log.d(TAG, "CREATED RECORDER AND AMPLITUDE SUCCESSFULLY")
		return true
	}

	/**
	 * Creates the file uri in which the audio to be recorded and initiate
	 * the recorder parameters
	 */
	@OptIn(ExperimentalCoroutinesApi::class)
	private suspend fun initiateRecorderParams() = coroutineScope {

		if (_recorder == null) {
			val isSuccess = createRecorder()
			if (!isSuccess) return@coroutineScope
		}

		// recorder should be ready by now
		val recorder = _recorder ?: return@coroutineScope
		// initiate the amplitude reader
		_reader.initiateRecorder()

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

		Log.d(TAG, "CONCURRENTLY FETCHING LOCATION AND PREPARING FILE")
		awaitAll(locationDeferred, fileDeferred)

		_recordingFile = fileDeferred.getCompleted()

		val locationResult = locationDeferred.getCompleted()

		recorder.apply {
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
			locationResult?.fold(
				onSuccess = { data ->
					Log.d(TAG, "LOCATION ADDED ")
					setLocation(
						data.latitude.toFloat(),
						data.longitude.toFloat()
					)
				},
				onFailure = { error -> Log.w(TAG, "${error.message}") },
			)
		}
		// metrics logs
		recorder.logMetrics()
	}

	/**
	 * Method to be called when recording has been finished, and you update the file
	 * metadata
	 */
	private suspend fun updateRecordingToExternalStorage(): Resource<Long?, Exception> {
		// update the file
		val recordingId = _recordingFile?.let { file ->
			withContext(Dispatchers.IO) {
				Log.d(TAG, "RECORDER FILE UPDATED")
				fileProvider.transferFileDataToStorage(file = file, mimeType = format.mimeType)
			}
		} ?: return Resource.Error(RecorderNotConfiguredException())
		// set recording uri to null and close the socket
		_recordingFile = null
		// resets the recorder for  next recording
		Log.d(TAG, "RESTING THE RECORDER")
		_recorder?.reset()
		_reader.releaseRecorder()
		return Resource.Success(recordingId)
	}

	private suspend fun stopAndDeleteFileMetaData() {
		// update the file
		_recordingFile?.let { file ->
			// non-cancellable as file should be deleted
			withContext(NonCancellable + Dispatchers.IO) {
				fileProvider.deleteCreatedFile(file)
				Log.d(TAG, "RECORDER FILE DELETED")
			}
		}
		// set recording uri to null and close the socket
		_recordingFile = null
		// resets the recorder for  next recording
		Log.d(TAG, "RESTING THE RECORDER")
		_recorder?.reset()
		_reader.releaseRecorder()
	}

	override suspend fun startRecording() {
		// if it's holding the lock don't do anything
		if (_lock.holdsLock(this)) {
			Log.d(TAG, "CANNOT START RECORDING ITS LOCKED")
			return
		}
		// current uri is already set cannot set it again
		if (_recordingFile != null) {
			Log.d(TAG, "CURRENT URI IS ALREADY SET")
			return
		}
		// staring an operation lock it
		_lock.lock(this)
		try {
			// prepare the recording params
			_stopWatch.prepare()
			Log.i(TAG, "PREPARING FILE FOR RECORDING")
			initiateRecorderParams()
			// prepare the recorder
			_recorder?.prepare()
			Log.d(TAG, "RECORDER PREPARED")
			//start the recorder
			_stopWatch.startOrResume()
			_reader.startRecorder()
			_recorder?.start()
			Log.d(TAG, "RECORDER STARTED")
		} catch (e: IOException) {
			e.printStackTrace()
		} finally {
			// unlocks the current lock
			_lock.unlock(this)
			Log.d(TAG, "CLEARING LOCK IN START")
		}
	}

	override suspend fun stopRecording(): Resource<Long?, Exception> {
		// if it's holding the lock don't do anything
		if (_lock.holdsLock(this)) {
			Log.d(TAG, "CANNOT STOP RECORDING ITS LOCKED")
			// returning null as there was no error but a lock
			return Resource.Success(null)
		}
		if (_recordingFile == null) {
			Log.d(TAG, "FILE URI IS NOT SET SO RECORDER IS NOT READY")
		}
		// staring an operation lock it
		_lock.lock(this)
		return try {
			// reset the timer
			Log.d(TAG, "STOPWATCH STOPPED")
			_stopWatch.stop()
			//stop the ongoing recording
			_recorder?.stop()
			Log.d(TAG, "RECORDER STOPPED")
			// update the file
			updateRecordingToExternalStorage()
		} catch (e: IllegalStateException) {
			e.printStackTrace()
			Resource.Error(e, message = "Cannot stop as start wasn't called")
		} finally {
			// unlocks the current lock
			_lock.unlock(this)
			Log.d(TAG, "CLEARING LOCK IN STOP")
		}
	}

	override suspend fun pauseRecording() {
		if (_lock.holdsLock(this)) {
			Log.d(TAG, "CANNOT PAUSE RECORDING")
			// returning null as there was no error but a lock
			return
		}
		// staring an operation lock it
		_lock.lock(this)
		try {
			//pause recorder
			Log.d(TAG, "STOPWATCH PAUSED")
			_stopWatch.pause()
			//pause recorder
			_recorder?.pause()
			Log.d(TAG, "RECORDER PAUSED")
		} catch (e: IOException) {
			e.printStackTrace()
		} finally {
			_lock.unlock()
		}
	}

	override suspend fun resumeRecording() {
		if (_lock.holdsLock(this)) {
			Log.d(TAG, "CANNOT RESUME RECORDING")
			// returning null as there was no error but a lock
			return
		}
		// staring an operation lock it
		_lock.lock(this)
		try {
			//resume stopwatch
			Log.d(TAG, "STOPWATCH RESUMED")
			_stopWatch.startOrResume()
			//resume recorder
			_recorder?.resume()
			Log.d(TAG, "RECORDER RESUMED")
		} catch (e: IOException) {
			e.printStackTrace()
		} finally {
			_lock.unlock()
		}
	}

	override suspend fun cancelRecording() {
		// if it's holding the lock don't do anything
		if (_lock.holdsLock(this)) {
			Log.d(TAG, "CANNOT CANCEL RECORDING ITS LOCKED")
			return
		}
		// staring an operation lock it
		_lock.lock(this)
		try {
			// cancel the timer watch
			Log.d(TAG, "STOPWATCH STOPPED")
			_stopWatch.cancel()
			//stop the ongoing recording
			Log.d(TAG, "RECORDER STOPPED")
			_recorder?.stop()
			// delete the current recording
			stopAndDeleteFileMetaData()
			Log.d(TAG, "RECORDER STOPPED")
		} catch (e: Exception) {
			e.printStackTrace()
		} finally {
			// unlocks the current lock
			_lock.unlock(this)
			Log.d(TAG, "CLEARING LOCK IN CANCEL")
		}
	}

	override fun releaseResources() {
		// delete the recording file if its exits
		_recordingFile?.let { file ->
			// run blocking as we want to run this blocking code in the IO thread.
			runBlocking {
				Log.d(TAG, "CLEARING THE FILE AS RECORDER CLEAR METHOD IS CALLED")
				fileProvider.deleteCreatedFile(file)
			}
		}
		//set recording file to null
		_recordingFile = null
		//set buffer reader to null
		_reader.releaseRecorder()
		// clear the recorder resources
		Log.d(TAG, "RELEASE RECORDER")
		_recorder?.release()
		_recorder = null
		// resetting the stopwatch
		Log.d(TAG, "RESETTING STOPWATCH")
		_stopWatch.reset()
	}
}

private fun MediaRecorder.logMetrics() {
	val currentMetrics = metrics ?: return
	val bitrate = currentMetrics.getInt(MediaRecorder.MetricsConstants.AUDIO_BITRATE)
	val sampleRte = currentMetrics.getInt(MediaRecorder.MetricsConstants.AUDIO_SAMPLERATE)
	val channel = currentMetrics.getInt(MediaRecorder.MetricsConstants.AUDIO_CHANNELS)
	Log.i(TAG, "RECORDER METRICS AFTER CONFIGURATION")
	Log.i(TAG, "SAMPLING RATE : $sampleRte")
	Log.i(TAG, "ENCODING BIT RATE : $bitrate")
	Log.i(TAG, "CHANNEL COUNT :$channel")

}