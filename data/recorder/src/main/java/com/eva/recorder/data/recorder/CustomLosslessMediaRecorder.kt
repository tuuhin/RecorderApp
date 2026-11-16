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
import com.eva.recorder_native.exception.InvalidHandleResultException
import com.eva.recorder_native.models.RecorderConfig
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

internal class CustomLosslessMediaRecorder(
    private val context: Context,
    private val fileProvider: RecorderFileProvider,
    private val settings: RecorderAudioSettingsRepo,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : GenericVoiceRecorder {

    @Volatile
    private var _recorder: CustomRecorder? = null

    @Volatile
    private var _recordingFile: File? = null

    override val isRecorderReady: StateFlow<Boolean>
        field = MutableStateFlow(false)

    override suspend fun prepareAndStart(onReady: suspend () -> Unit): Result<Unit> {

        // check permissions and file
        if (!context.hasAudioRecordPermission) return Result.failure(IllegalStateException("Audio recording permissions missing"))
        if (_recordingFile != null) return Result.failure(IllegalStateException("A recording file is already configured"))

        return withContext(dispatcher) {
            try {
                // recorder setup completed
                initRecordingParams()
                val recorder = _recorder ?: throw IllegalStateException("Recorder cannot be prepared")
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
        val recorder = _recorder ?: return@withContext
        Log.d(TAG, "PAUSING CUSTOM RECORDER INSTANCE")
        recorder.pause()
    }


    override suspend fun resumeRecording() = withContext(dispatcher) {
        val recorder = _recorder ?: return@withContext
        Log.d(TAG, "RESUMING CUSTOM RECORDER INSTANCE")
        recorder.resume()
    }

    override suspend fun cancelOngoingRecording() {
        Log.d(TAG, "RECORDING CANCELLED")

        return withContext(dispatcher) {
            try {
                _recorder?.stop()
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
        val recorder = _recorder
            ?: throw IllegalStateException("Custom recorder is not created cannot configure it")

        // ensures the file is being created in a different coroutine
        val fileDeferred = async(dispatcher) {
            fileProvider.createFileForRecording(format.extension)
        }
        _recordingFile = fileDeferred.await()

        // TODO: 48khz is working for now need to test other sample rates
        val config = RecorderConfig(sampleRate = 48_000, channel = channelCount)

        recorder.apply {
            setOutputFile(fileDeferred.await())
            setRecorderConfig(config = config)
            onError = { err ->
                Log.e(TAG, "Custom recorder reported an error", err)
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
        withContext(dispatcher) {
            try {
                _recorder?.create()
            } catch (e: CancellationException) {
                throw e
            } catch (e: InvalidHandleResultException) {
                Log.e(TAG, "RECORDER HANDLE CANNOT BE ALLOCATED", e)
            }
        }
        Log.d(TAG, "CREATED RECORDER SUCCESSFULLY")
        return true
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
