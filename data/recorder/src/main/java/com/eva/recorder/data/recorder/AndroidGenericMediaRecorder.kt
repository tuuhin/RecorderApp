package com.eva.recorder.data.recorder

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.eva.datastore.domain.repository.RecorderAudioSettingsRepo
import com.eva.location.domain.repository.LocationProvider
import com.eva.recorder.data.hasAudioRecordPermission
import com.eva.recorder.data.models.AndroidGenricRecordFormats
import com.eva.recorder.domain.GenericVoiceRecorder
import com.eva.recorder.domain.exceptions.RecorderNotConfiguredException
import com.eva.recordings.domain.provider.RecorderFileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class AndroidGenericMediaRecorder(
    private val context: Context,
    private val fileProvider: RecorderFileProvider,
    private val settings: RecorderAudioSettingsRepo,
    private val locationProvider: LocationProvider,
) : GenericVoiceRecorder, MediaRecorder.OnErrorListener {

    @Volatile
    private var _recorder: MediaRecorder? = null

    @Volatile
    private var _recordingFile: File? = null

    override val isRecorderReady: StateFlow<Boolean>
        field = MutableStateFlow(false)

    override fun onError(p0: MediaRecorder?, what: Int, extra: Int) {
        if (what == MediaRecorder.MEDIA_ERROR_SERVER_DIED) release()
        Log.w(TAG, "SOME ERROR OCCURRED :$what CODE: $extra")
    }

    override suspend fun prepareAndStart(onReady: suspend () -> Unit): Result<Unit> = runCatching {

        if (!context.hasAudioRecordPermission) throw IllegalStateException("Audio recording permissions missing")
        if (_recordingFile != null) throw IllegalStateException("A recording file is already configured")

        initRecorderWithParams()
        val recorder = _recorder ?: error("MediaRecorder could not be created")
        recorder.prepare()
        Log.d(TAG, "RECORDER PREPARED")
        onReady()
        recorder.start()
        Log.d(TAG, "RECORDER STARTED")
        isRecorderReady.value = true

    }.onFailure { err ->
        isRecorderReady.value = false
        withContext(NonCancellable) {
            _recordingFile?.let { fileProvider.deleteCreatedFile(it) }
            _recordingFile = null
        }
        _recorder?.release()
        _recorder = null
        Log.e(TAG, "FAILED TO START THE RECORDER", err)
    }


    override suspend fun stopRecording(): Result<File> = runCatching {
        val file = _recordingFile ?: throw RecorderNotConfiguredException()
        val recorder = _recorder ?: error("MediaRecorder could not be created")
        Log.d(TAG, "STOPWATCH STOPPED")
        //stop the ongoing recording
        try {
            recorder.stop()
            Log.d(TAG, "RECORDER STOPPED")
            file
        } catch (err: Exception) {
            withContext(NonCancellable) {
                fileProvider.deleteCreatedFile(file)
            }
            recorder.reset()
            throw err
        } finally {
            _recordingFile = null
            isRecorderReady.value = false
        }
    }

    override suspend fun pauseRecording(): Result<Unit> = runCatching {
        val recorder = _recorder ?: error("MediaRecorder could not be created")
        recorder.pause()
        Log.d(TAG, "RECORDER PAUSED")
    }

    override suspend fun discardRecordingFile(file: File) {
        if (file.exists()) fileProvider.deleteCreatedFile(file)
    }

    override suspend fun resumeRecording() {
        val recorder = _recorder ?: throw RecorderNotConfiguredException()
        recorder.resume()
        Log.d(TAG, "RECORDER RESUMED")
    }

    override suspend fun cancelOngoingRecording() {
        val recorder = _recorder ?: throw RecorderNotConfiguredException()
        val file = _recordingFile
        try {
            recorder.stop()
        } finally {
            _recordingFile = null
            isRecorderReady.value = false
            recorder.reset()
            withContext(NonCancellable) {
                Log.d(TAG, "CLEARING THE FILE DUE TO CANCEL")
                file?.let { if (it.exists()) fileProvider.deleteCreatedFile(it) }
            }
        }
    }

    override fun release() {
        // delete the recording file if its exits
        try {
            val file = _recordingFile ?: return
            // run blocking as we want to run this blocking code in the IO thread.
            runBlocking {
                withContext(NonCancellable) {
                    Log.d(TAG, "CLEARING THE FILE AS RECORDER CLEAR METHOD IS CALLED")
                    if (file.exists()) fileProvider.deleteCreatedFile(file)
                }
            }
        } finally {
            //set recording file to null
            _recordingFile = null
            isRecorderReady.value = false
            Log.d(TAG, "RELEASE RECORDER")
            _recorder?.release()
            _recorder = null
        }
    }


    private suspend fun initRecorderWithParams() = coroutineScope {

        if (_recorder == null) {
            val isSuccess = create()
            if (!isSuccess) throw IllegalStateException("MEDIA RECORDER CANNOT BE CREATED")
        } else {
            Log.d(TAG, "RECORDER ALREADY FOUND RESTING IT")
            _recorder?.reset()
        }

        val audioSettings = settings.audioSettings()
        val format = AndroidGenricRecordFormats.fromEncoder(audioSettings.encoders)
        val quality = audioSettings.quality
        val channelCount = if (audioSettings.enableStereo) 2 else 1

        // recorder should be ready by now
        val recorder = _recorder ?: error("MediaRecorder could not be created")

        // ensures the file is being created in a different coroutine
        val fileDeferred = async(Dispatchers.IO) {
            fileProvider.createFileForRecording()
        }

        // location deferred for current location if available
        val locationDeferred = async(Dispatchers.IO) {
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
            Log.w(TAG, "CANNOT FETCH LOCATION TO SAVE", locationResult.exceptionOrNull())
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
                val location = locationResult.getOrThrow()
                setLocation(
                    location.latitude.toFloat(),
                    location.longitude.toFloat(),
                )
            }
        }
        recorder.logMetrics()
    }

    fun create(): Boolean {
        // no perms granted
        if (!context.hasAudioRecordPermission) {
            Log.i(TAG, "RECORD AUDIO PERMISSION MISSING")
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
        else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        _recorder?.setOnErrorListener(this)
        Log.d(TAG, "CREATED RECORDER SUCCESSFULLY")
        return true
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

    companion object {
        private const val TAG = "LossyVoiceRecorder"
    }
}
