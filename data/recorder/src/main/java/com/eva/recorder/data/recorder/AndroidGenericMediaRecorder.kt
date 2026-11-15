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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class AndroidGenericMediaRecorder(
    private val context: Context,
    private val fileProvider: RecorderFileProvider,
    private val settings: RecorderAudioSettingsRepo,
    private val locationProvider: LocationProvider,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
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

    override suspend fun prepareAndStart(onReady: suspend () -> Unit): Result<Unit> {
        // check permissions and file
        if (!context.hasAudioRecordPermission) return Result.failure(IllegalStateException("Audio recording permissions missing"))
        if (_recordingFile != null) return Result.failure(IllegalStateException("A recording file is already configured"))

        return withContext(dispatcher) {
            try {
                // recorder setup completed
                initRecorderWithParams()
                val recorder = _recorder ?: throw IllegalStateException("MediaRecorder could not be created")
                // recorder prep completed
                recorder.prepare()
                Log.d(TAG, "RECORDER PREPARED")
                // external preparation and ensure coroutine is still active
                onReady()
                currentCoroutineContext().ensureActive()
                // start recorder
                recorder.start()
                Log.d(TAG, "RECORDER STARTED")
                isRecorderReady.value = true
                Result.success(Unit)
            } catch (e: Exception) {
                withContext(NonCancellable) {
                    try {
                        Log.d(TAG, "OPERATION FAILED NEED TO DELETE THE FINLE")
                        _recordingFile?.let { fileProvider.deleteCreatedFile(it) }
                    } finally {
                        _recordingFile = null
                        isRecorderReady.value = false
                        safeRelease()
                    }
                }
                if (e is CancellationException) {
                    Log.w(TAG, "START WAS CANCELLED")
                    throw e
                }
                Log.e(TAG, "FAILED TO START THE RECORDER", e)
                Result.failure(e)
            }
        }
    }


    override suspend fun stopRecording(): Result<File> {
        Log.d(TAG, "STOPPING RECORDER")

        val file = _recordingFile ?: return Result.failure(RecorderNotConfiguredException())
        val recorder = _recorder ?: return Result.failure(IllegalStateException("Recorder is not ready"))

        return withContext(dispatcher) {
            try {
                recorder.stop()
                recorder.reset()
                Log.i(TAG, "RECORDER STOP AND RESET APPROVED")
                Result.success(file)
            } catch (e: Exception) {
                Log.w(TAG, "STOP FAILED DUE WITH EXCEPTION DELETING RECORDING", e)
                withContext(NonCancellable) {
                    fileProvider.deleteCreatedFile(file)
                }
                if (e is CancellationException) throw e
                Result.failure(e)
            } finally {
                Log.d(TAG, "RESTING RECORDER")
                _recordingFile = null
                isRecorderReady.value = false
                safeRecorderReset()
            }
        }
    }

    override suspend fun pauseRecording() = withContext(dispatcher) {
        val recorder = _recorder ?: error("MediaRecorder could not be created")
        recorder.pause()
        Log.d(TAG, "RECORDER PAUSED")
        Unit
    }


    override suspend fun resumeRecording() = withContext(dispatcher) {
        val recorder = _recorder ?: throw RecorderNotConfiguredException()
        recorder.resume()
        Log.d(TAG, "RECORDER RESUMED")
        Unit
    }

    override suspend fun cancelOngoingRecording() {
        val recorder = _recorder ?: throw RecorderNotConfiguredException()

        return withContext(dispatcher) {
            try {
                recorder.stop()
                _recordingFile?.let {
                    Log.d(TAG, "CLEARING THE FILE DUE TO CANCEL")
                    if (it.exists()) fileProvider.deleteCreatedFile(it)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "RECORDING CANCELLATION FAILED", e)
            } finally {
                withContext(NonCancellable) {
                    _recordingFile = null
                    isRecorderReady.value = false
                    safeRecorderReset()
                }
            }
        }
    }

    override fun release() {
        // delete the recording file if its exits
        try {
            val file = _recordingFile ?: return
            // run blocking as we want to run this blocking code in the IO thread.
            runBlocking(Dispatchers.IO) {
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

    private suspend fun safeRelease() {
        val recorder = _recorder ?: return
        withContext(dispatcher) {
            try {
                recorder.release()
                _recorder = null
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "FAILED TO RELEASE THE RECORDER")
            }
        }
    }

    private suspend fun safeRecorderReset() {
        val recorder = _recorder ?: return
        withContext(dispatcher) {
            try {
                recorder.reset()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "FAILED TO RESET THE RECORDER")
            }
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
        val fileDeferred = async(dispatcher) {
            fileProvider.createFileForRecording()
        }

        // location deferred for current location if available
        val locationDeferred = async(dispatcher) {
            // if settings is enabled
            if (!audioSettings.addLocationInfoInRecording) return@async null
            // if format supports it
            if (format.outputFormat != MediaRecorder.OutputFormat.THREE_GPP && format.outputFormat != MediaRecorder.OutputFormat.MPEG_4) return@async null
            locationProvider.invoke()
        }

        Log.d(TAG, "CONCURRENTLY FETCHING LOCATION AND PREPARING FILE")
        _recordingFile = fileDeferred.await()
        Log.d(TAG, "RECORDING FILE IS READY")

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
