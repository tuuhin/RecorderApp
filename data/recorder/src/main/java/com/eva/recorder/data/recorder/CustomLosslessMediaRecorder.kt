package com.eva.recorder.data.recorder

import android.content.Context
import android.util.Log
import com.eva.datastore.domain.enums.RecordingEncoders
import com.eva.datastore.domain.repository.RecorderAudioSettingsRepo
import com.eva.recorder.data.hasAudioRecordPermission
import com.eva.recorder.data.models.CustomRecordingFormats
import com.eva.recorder.domain.GenericVoiceRecorder
import com.eva.recorder.domain.exceptions.RecorderNotConfiguredException
import com.eva.recorder_native.CustomRecorder
import com.eva.recorder_native.FlacAudioRecorder
import com.eva.recorder_native.WavAudioRecorder
import com.eva.recorder_native.models.RecorderConfig
import com.eva.recordings.domain.provider.RecorderFileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

internal class CustomLosslessMediaRecorder(
    private val context: Context,
    private val fileProvider: RecorderFileProvider,
    private val settings: RecorderAudioSettingsRepo,
) : GenericVoiceRecorder {

    @Volatile
    private var _recorder: CustomRecorder? = null

    @Volatile
    private var _recordingFile: File? = null

    override val isRecorderReady: StateFlow<Boolean>
        field = MutableStateFlow(false)

    override suspend fun prepareAndStart(onReady: suspend () -> Unit): Result<Unit> = runCatching {
        if (!context.hasAudioRecordPermission) throw IllegalStateException("Audio recording permissions missing")
        if (_recordingFile != null) throw IllegalStateException("A recording file is already configured")

        initRecordingParams()
        val recorder = _recorder ?: throw IllegalStateException("MediaRecorder could not be created")
        recorder.prepare()
        Log.d(TAG, "RECORDER PREPARED")
        onReady()
        recorder.start()
        Log.d(TAG, "RECORDER STARTED")
        isRecorderReady.value = true
    }.onFailure { err ->
        isRecorderReady.value = false
        withContext(NonCancellable) {
            Log.w(TAG, "FAILED TO START THE RECORDER ")
            _recordingFile?.let { fileProvider.deleteCreatedFile(it) }
            _recordingFile = null
        }
        _recorder?.release()
        _recorder = null
        Log.e(TAG, "FAILED TO START THE RECORDER", err)
    }

    override suspend fun stopRecording(): Result<File> = runCatching {
        val file = _recordingFile ?: throw RecorderNotConfiguredException()
        val recorder = _recorder ?: throw IllegalStateException("MediaRecorder could not be created")
        try {
            recorder.stop()
            recorder.reset()
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
        val recorder = _recorder ?: throw IllegalStateException("MediaRecorder could not be created")
        recorder.pause()
    }

    override suspend fun discardRecordingFile(file: File) {
        if (file.exists()) fileProvider.deleteCreatedFile(file)
    }

    override suspend fun resumeRecording() {
        val recorder = _recorder ?: throw IllegalStateException("MediaRecorder could not be created")
        recorder.resume()
    }

    override suspend fun cancelOngoingRecording() {
        val recorder = _recorder ?: throw IllegalStateException("MediaRecorder could not be created")
        val file = _recordingFile
        try {
            recorder.cancel()
        } finally {
            withContext(NonCancellable) {
                file?.let { if (it.exists()) fileProvider.deleteCreatedFile(it) }
            }
            recorder.reset()
            _recordingFile = null
            isRecorderReady.value = false
        }
    }

    override fun release() {
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

    private suspend fun initRecordingParams() = coroutineScope {

        if (_recorder == null) {
            val isSuccess = createRecorderInstance()
            if (!isSuccess) throw IllegalStateException("FAILED TO PREPARE THE RECORDER")
        } else {
            Log.d(TAG, "RECORDER ALREADY FOUND RESTING IT")
            _recorder?.reset()
        }

        val audioSettings = settings.audioSettings()

        val format = when (audioSettings.encoders) {
            RecordingEncoders.WAV -> CustomRecordingFormats.WAV
            RecordingEncoders.FLAC -> CustomRecordingFormats.FLAC
            else -> throw IllegalArgumentException("Custom media recorder only supports WAV and FLAC formats")
        }

        val channelCount = if (audioSettings.enableStereo) 2 else 1

        // recorder should be ready by now
        val recorder = _recorder ?: throw IllegalStateException("MediaRecorder could not be created")

        // ensures the file is being created in a different coroutine
        val fileDeferred = async(Dispatchers.IO) {
            fileProvider.createFileForRecording(format.extension)
        }
        _recordingFile = fileDeferred.await()
        val config = RecorderConfig(channel = channelCount)

        recorder.apply {
            setOutputFile(fileDeferred.await())
            setRecorderConfig(config = config)
            onError = { err ->
                Log.w(TAG, "FAILED TO SET THE RECORDER ", err)
                release()
            }
        }
        config.logMetrics()
    }

    suspend fun createRecorderInstance(): Boolean {
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

        val settings = settings.audioSettings()
        val encoder = settings.encoders
        // set recorder
        _recorder = when (encoder) {
            RecordingEncoders.WAV -> WavAudioRecorder()
            RecordingEncoders.FLAC -> FlacAudioRecorder()
            else -> throw IllegalArgumentException("Custom media recorder only supports WAV and FLAC formats")
        }
        // create the recorder instance
        _recorder?.create()
        Log.d(TAG, "CREATED RECORDER SUCCESSFULLY")
        return true
    }

    private fun RecorderConfig.logMetrics() {
        Log.i(TAG, "RECORDER METRICS AFTER CONFIGURATION")
        Log.d(TAG, "CURRENT RECORDER :${_recorder?.javaClass?.simpleName}")
        Log.i(TAG, "SAMPLING RATE : $sampleRate")
        Log.i(TAG, "CHANNEL COUNT :$channel")
    }

    companion object {
        private const val TAG = "CustomLosslessMediaRecorder"
    }
}
