package com.eva.recorderapp.data.voice_recorder

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
import androidx.core.net.toFile
import com.eva.recorderapp.domain.voice_recorder.RecorderFileProvider
import com.eva.recorderapp.domain.voice_recorder.VoiceRecorder
import java.io.IOException

private const val LOGGER_TAG = "RECORDER_TAG"

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


	@Suppress("DEPRECATION")
	private fun createRecorder() {
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
		_recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
			MediaRecorder(context)
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
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}

	override suspend fun stopRecording() {
		try {
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
			_recorder?.pause()
			Log.d(LOGGER_TAG, "RECORDER PAUSED")
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}

	override fun resumeRecording() {
		try {
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
	}
}
