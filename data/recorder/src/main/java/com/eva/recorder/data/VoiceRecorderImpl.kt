package com.eva.recorder.data

import android.Manifest
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.eva.datastore.domain.repository.RecorderAudioSettingsRepo
import com.eva.location.domain.repository.LocationProvider
import com.eva.recorder.data.reader.AudioRecordAmplitudeReader
import com.eva.recorder.domain.VoiceRecorder
import com.eva.recorder.domain.exceptions.RecorderNotConfiguredException
import com.eva.recorder.domain.models.RecordedPoint
import com.eva.recorder.domain.models.RecorderState
import com.eva.recorder.domain.stopwatch.RecorderStopWatch
import com.eva.recordings.domain.provider.RecorderFileProvider
import com.eva.utils.RecorderConstants
import com.eva.utils.tryWithLock
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
import kotlinx.coroutines.sync.withLock
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

	private val sampleTime = RecorderConstants.AMPS_READ_DELAY_RATE

	private val _stopWatch = RecorderStopWatch(delayTime = sampleTime)

	private val _pcmReader by lazy {
		AudioRecordAmplitudeReader(
			context = context,
			stopWatch = _stopWatch,
			delayRate = sampleTime
		)
	}

	private var _recorder: MediaRecorder? = null

	@Volatile
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
			.flatMapLatest(_pcmReader::readAmplitudeBuffered)

	private val errorListener = MediaRecorder.OnErrorListener { _, what, extra ->
		if (what == MediaRecorder.MEDIA_ERROR_SERVER_DIED) releaseResources()
		Log.w(TAG, "SOME ERROR OCCURRED :$what CODE: $extra")
	}

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

		_recorder?.setOnErrorListener(errorListener)
		Log.d(TAG, "CREATED RECORDER AND AMPLITUDE SUCCESSFULLY")
		return true
	}

	/**
	 * Creates the file uri in which the audio to be recorded and initiate
	 * the recorder parameters
	 */
	private suspend fun initiateRecorderParams() = coroutineScope {

		if (_recorder == null) {
			val isSuccess = createRecorder()
			if (!isSuccess) return@coroutineScope
		}

		val audioSettings = settings.audioSettings()
		val format = RecordFormats.fromEncoder(audioSettings.encoders)
		val quality = audioSettings.quality
		val channelCount = if (audioSettings.enableStereo) 2 else 1

		// recorder should be ready by now
		val recorder = _recorder ?: return@coroutineScope
		// initiate the amplitude reader
		_pcmReader.initiateRecorder(
			sampleRate = quality.sampleRate,
			isStereo = audioSettings.enableStereo
		)

		// ensures the file is being created in a different coroutine
		val fileDeferred = async {
			fileProvider.createFileForRecording(format.fileExtension)
		}

		// location deferred for current location if available
		val locationDeferred = async {
			// if settings is enabled
			if (!audioSettings.addLocationInfoInRecording) return@async null
			// if format supports it
			if (format.outputFormat != MediaRecorder.OutputFormat.THREE_GPP && format.outputFormat != MediaRecorder.OutputFormat.MPEG_4) return@async null
			locationProvider.invoke()
		}

		Log.d(TAG, "CONCURRENTLY FETCHING LOCATION AND PREPARING FILE")
		awaitAll(locationDeferred, fileDeferred)
		Log.d(TAG, "DEFERRED CALL ARE READY")

		_recordingFile = fileDeferred.await()

		val locationResult = locationDeferred.await()
		// log if any location error
		if (locationResult?.isFailure == true) {
			Log.w(TAG, "Location Cannot be fetched", locationResult.exceptionOrNull())
		}

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
			if (locationResult != null && locationResult.isSuccess) {
				val location = locationResult.getOrNull() ?: return@apply
				setLocation(
					location.latitude.toFloat(),
					location.longitude.toFloat()
				)
			}
		}
		recorder.logMetrics()
	}

	/**
	 * Method to be called when recording has been finished, and you update the file
	 * metadata
	 */
	private suspend fun updateRecordingToExternalStorage(recordingFile: File): Result<Long> {
		// update the file
		try {
			val audioSettings = settings.audioSettings()
			val format = RecordFormats.fromEncoder(audioSettings.encoders)

			Log.d(TAG, "RECORDER FILE UPDATED")
			return fileProvider.transferFileDataToStorage(
				file = recordingFile,
				mimeType = format.mimeType
			)
		} finally {
			// set recording uri to null and close the socket
			_recordingFile = null
			// resets the recorder for  next recording
			Log.d(TAG, "RESTING THE RECORDER")
			_recorder?.reset()
			_pcmReader.releaseRecorder()
		}
	}

	private suspend fun stopAndDeleteFileMetaData() {
		// update the file
		try {
			val file = _recordingFile ?: return
			// non-cancellable as file should be deleted
			withContext(NonCancellable) {
				fileProvider.deleteCreatedFile(file)
				Log.d(TAG, "RECORDER FILE DELETED")
			}
		} finally {
			// set recording uri to null and close the socket
			_recordingFile = null
			// resets the recorder for  next recording
			Log.d(TAG, "RESTING THE RECORDER")
			_recorder?.reset()
			_pcmReader.releaseRecorder()
		}
	}

	override suspend fun startRecording() {
		_lock.tryWithLock(this) {
			// current uri is already set cannot set it again
			if (_recordingFile != null) {
				Log.d(TAG, "CURRENT URI IS ALREADY SET")
				return@tryWithLock
			}
			_stopWatch.prepare()
			Log.i(TAG, "PREPARING FILE FOR RECORDING")
			initiateRecorderParams()
			// prepare the recorder
			_recorder?.prepare()
			Log.d(TAG, "RECORDER PREPARED")
			//start the recorder
			_stopWatch.startOrResume()
			_pcmReader.startRecorder()
			_recorder?.start()
			Log.d(TAG, "RECORDER STARTED")
		}
	}

	override suspend fun stopRecording(): Result<Long> {
		// staring an operation lock it
		return _lock.withLock(this) {
			val file = _recordingFile ?: return Result.failure(RecorderNotConfiguredException())
			// reset the timer
			Log.d(TAG, "STOPWATCH STOPPED")
			_stopWatch.stop()
			//stop the ongoing recording
			_recorder?.stop()
			Log.d(TAG, "RECORDER STOPPED")
			Result.success(0L)
			// update the file
			updateRecordingToExternalStorage(file)
		}
	}

	override suspend fun pauseRecording() {
		_lock.tryWithLock(this) {
			try {
				//pause recorder
				Log.d(TAG, "STOPWATCH PAUSED")
				_stopWatch.pause()
				//pause recorder
				_recorder?.pause()
				Log.d(TAG, "RECORDER PAUSED")
			} catch (e: IOException) {
				e.printStackTrace()
			}
		}
	}

	override suspend fun resumeRecording() {
		_lock.tryWithLock(this) {
			try {
				//resume stopwatch
				Log.d(TAG, "STOPWATCH RESUMED")
				_stopWatch.startOrResume()
				//resume recorder
				_recorder?.resume()
				Log.d(TAG, "RECORDER RESUMED")
			} catch (e: IOException) {
				e.printStackTrace()
			}
		}
	}

	override suspend fun cancelRecording() {
		// if it's holding the lock don't do anything
		_lock.tryWithLock(this) {
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
			}
		}
	}

	override fun releaseResources() {
		// delete the recording file if its exits
		try {
			val file = _recordingFile ?: return
			// run blocking as we want to run this blocking code in the IO thread.
			runBlocking {
				withContext(NonCancellable) {
					Log.d(TAG, "CLEARING THE FILE AS RECORDER CLEAR METHOD IS CALLED")
					fileProvider.deleteCreatedFile(file)
				}
			}
		} finally {
			//set recording file to null
			_recordingFile = null
			//set buffer reader to null
			_pcmReader.releaseRecorder()
			// clear the recorder resources
			Log.d(TAG, "RELEASE RECORDER")
			_recorder?.release()
			_recorder = null
			// resetting the stopwatch
			Log.d(TAG, "RESETTING STOPWATCH")
			_stopWatch.reset()
		}
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