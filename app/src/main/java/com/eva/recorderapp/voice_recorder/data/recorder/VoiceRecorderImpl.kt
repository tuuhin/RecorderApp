package com.eva.recorderapp.voice_recorder.data.recorder

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.eva.recorderapp.voice_recorder.data.util.flowToFixedSizeCollection
import com.eva.recorderapp.voice_recorder.data.util.toNormalizedValues
import com.eva.recorderapp.voice_recorder.domain.emums.RecorderState
import com.eva.recorderapp.voice_recorder.domain.recorder.RecorderFileProvider
import com.eva.recorderapp.voice_recorder.domain.recorder.VoiceRecorder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalTime
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

private const val LOGGER_TAG = "VOICE_RECORDER"

class VoiceRecorderImpl(
	private val context: Context,
	private val fileProvider: RecorderFileProvider,
) : VoiceRecorder {

	private val contentResolver: ContentResolver
		get() = context.contentResolver

	private var _recorder: MediaRecorder? = null
	private var _recordingUri: Uri? = null

	val _hasRecordPremission: Boolean
		get() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
				PermissionChecker.PERMISSION_GRANTED

	private val _isAudioSourceConfigured: Boolean?
		get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			// not null ensures its set
			_recorder?.activeRecordingConfiguration?.audioSource != null
		} else null

	private var _recorderState = MutableStateFlow(RecorderState.IDLE)
	private var _elapsedTime = MutableStateFlow(0L)

	override val recorderState: StateFlow<RecorderState>
		get() = _recorderState.asStateFlow()

	@OptIn(ExperimentalCoroutinesApi::class)
	override val recorderTimer: Flow<LocalTime>
		get() = _recorderState.flatMapLatest(::runStopWatch)
			.map { diff ->
				val nanos = _elapsedTime.updateAndGet { now -> now + diff }
				LocalTime.fromNanosecondOfDay(nanos)
			}

	@OptIn(ExperimentalCoroutinesApi::class)
	override val maxAmplitudes: Flow<FloatArray>
		get() = _recorderState.flatMapLatest(::readSampledAmplitude)
			.flowToFixedSizeCollection(80)
			.toNormalizedValues()
			.map { values -> values.reversed().toFloatArray() }


	private fun readSampledAmplitude(state: RecorderState): Flow<Int> {
		return flow {
			try {
				while (_recorder != null && state == RecorderState.RECORDING) {
					// check if audio source set
					if (_isAudioSourceConfigured == false) break
					// record the max amplitude of the sample
					val amplitude = _recorder?.maxAmplitude ?: continue
					emit(amplitude)
					delay(20.milliseconds)
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
		}.flowOn(Dispatchers.Default)
	}

	private fun runStopWatch(state: RecorderState): Flow<Int> {
		return flow {
			while (state == RecorderState.RECORDING) {
				val previous = Clock.System.now().nanosecondsOfSecond
				delay(100.milliseconds)
				val now = Clock.System.now().nanosecondsOfSecond
				val diff = if (now > previous) now - previous else 0
				emit(diff)
			}
		}.flowOn(Dispatchers.Default)
	}

	@Suppress("DEPRECATION")
	override fun createRecorder() {
		// no perms granted
		if (!_hasRecordPremission) {
			Log.d(LOGGER_TAG, "NO RECORD PERMISSION FOUND")
			return
		}
		// recorder already set
		if (_recorder != null) {
			Log.d(LOGGER_TAG, "RECORDER ALREADY INITIATED")
			return
		}
		// set recorder
		_recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
		else MediaRecorder()

		Log.d(LOGGER_TAG, "CREATED RECORDER SUCCESSFULLY")
	}

	/**
	 * Re-Initiate the recorder params
	 * Once stop or reset this method to be called to reset the params
	 */
	@SuppressLint("Recycle")
	private suspend fun initiateRecorderParams() {
		if (_recorder == null) createRecorder()

		_recordingUri = fileProvider.createFileForRecording() ?: return

		Log.d(LOGGER_TAG, "NEW_FILE_URI_CREATED")

		// it will be closed when stoped
		// TODO: Check it later if this is being closed properly??
		val fd = contentResolver.openFileDescriptor(_recordingUri!!, "w") ?: return

		_recorder?.apply {
			setAudioSource(MediaRecorder.AudioSource.MIC)
			setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
			setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
			setOutputFile(fd.fileDescriptor)
		}

		Log.d(LOGGER_TAG, "RECORDER CONFIGURED")
	}

	/**
	 * Method to be called when recording has been finished and you update the file
	 * metadata
	 */
	private suspend fun updateRecorderParams() {
		_recordingUri?.let { uri ->
			fileProvider.updateFileAfterRecording(uri)
			Log.d(LOGGER_TAG, "RECORDER FILE UPDATED")
		}
		// new file required
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
			//set is recording to true
			_recorderState.update { RecorderState.RECORDING }
			Log.d(LOGGER_TAG, "IS RECORDING TO TRUE")
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}

	override suspend fun stopRecording() {
		try {
			// reset the timer
			Log.d(LOGGER_TAG, "RESET THE STOPWATCH")
			_elapsedTime.update { 0 }
			//set is recording to completed
			_recorderState.update { RecorderState.COMPLETED }
			Log.d(LOGGER_TAG, "SETTING IS RECORDING TO FALSE")
			//stop the ongoing recording
			_recorder?.stop()
			Log.d(LOGGER_TAG, "RECORDER STOPPED")
			// update the file
			updateRecorderParams()
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}

	override fun pauseRecording() {
		try {
			_recorderState.update { RecorderState.PAUSED }
			_recorder?.pause()
			Log.d(LOGGER_TAG, "RECORDER PAUSED")
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}

	override fun resumeRecording() {
		try {
			_recorderState.update { RecorderState.RECORDING }
			_recorder?.resume()
			Log.d(LOGGER_TAG, "RECORDER RESUMED")
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}

	override suspend fun releaseResources() {
		_recordingUri?.let {
			// clear the file if not save correctlty
			Log.d(LOGGER_TAG, "CLEARING THE FILE AS RECORDER CLEAR METHOD IS CALLED")
			fileProvider.cancelFileCreation(it)
		}
		Log.d(LOGGER_TAG, "RELEASE RECORDER")
		// clear the recorder resources
		_recorder?.release()
		_recorder == null
		// set is recording to false
		Log.d(LOGGER_TAG, "RECORDER STATE TO IDLE")
		_recorderState.update { RecorderState.IDLE }
		// reset the timer
		Log.d(LOGGER_TAG, "RESET THE STOPWATCH")
		_elapsedTime.update { 0 }
	}
}