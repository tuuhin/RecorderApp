package com.eva.recorder.data.recorder

import android.util.Log
import com.eva.datastore.domain.enums.RecordingEncoders
import com.eva.datastore.domain.repository.RecorderAudioSettingsRepo
import com.eva.recorder.data.models.AndroidGenricRecordFormats
import com.eva.recorder.data.models.CustomRecordingFormats
import com.eva.recorder.domain.GenericVoiceRecorder
import com.eva.recorder.domain.models.RecorderState
import com.eva.recorder.domain.recorder.AudioByteDataProvider
import com.eva.recorder.domain.recorder.AudioDataReader
import com.eva.recorder.domain.recorder.VoiceRecorder
import com.eva.recorder.domain.stopwatch.RecorderStopWatch
import com.eva.recordings.domain.provider.RecorderFileProvider
import com.eva.utils.tryWithLock
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalTime

private const val TAG = "VOICE_RECORDER"

@OptIn(ExperimentalCoroutinesApi::class)
internal class VoiceRecorderImpl(
    private val stopWatch: RecorderStopWatch,
    private val fileProvider: RecorderFileProvider,
    private val settings: RecorderAudioSettingsRepo,
    private val pcmReader: AudioDataReader,
    private val androidRecorder: GenericVoiceRecorder,
    private val customRecorder: GenericVoiceRecorder,
) : VoiceRecorder, AudioByteDataProvider {

    private val _scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var _recorder: GenericVoiceRecorder? = null

    // locks ensures an operation complete before another operation can start
    private val _lock = Mutex()

    override val recorderState: StateFlow<RecorderState>
        get() = stopWatch.recorderState

    override val recorderTimer: StateFlow<LocalTime>
        get() = stopWatch.elapsedTime

    override val stream: Flow<Pair<ShortArray, Int>> = stopWatch.recorderState
        .flatMapLatest { pcmReader.readRecorderRawBytes(it) }
        .shareIn(_scope, SharingStarted.Lazily, replay = 0)


    private suspend fun updateRecordingToExternalStorage(recordingFile: File): Result<Long> {
        try {
            val settings = settings.audioSettings()
            val mimeType = when (val encoder = settings.encoders) {
                RecordingEncoders.WAV, RecordingEncoders.FLAC -> CustomRecordingFormats.fromEncoder(encoder).mimeType
                else -> AndroidGenricRecordFormats.fromEncoder(encoder).mimeType
            }

            Log.d(TAG, "COPYING THE RECORDED FILE")
            return fileProvider.transferFileDataToStorage(recordingFile, mimeType)
        } finally {
            Log.d(TAG, "RELEASING RECORDING RESOURCES")
            pcmReader.releaseRecorder()
        }
    }


    override suspend fun startRecording() {
        _lock.tryWithLock(this) {

            Log.i(TAG, "PREPARING FILE FOR RECORDING")
            try {
                // sets the recorder
                val recorder = setupRecorder()

                stopWatch.prepare()
                recorder.prepareAndStart {
                    pcmReader.initiateRecorder()
                    pcmReader.startRecorder()
                }.getOrThrow()
                stopWatch.startOrResume()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                stopWatch.cancel()
                pcmReader.releaseRecorder()
            }
            Log.d(TAG, "RECORDER STARTED")
        }
    }


    override suspend fun stopRecording(): Result<Long> {
        // staring an operation lock it
        return _lock.withLock(this) {
            val recorder = _recorder ?: throw IllegalStateException("MediaRecorder could not be created")
            //stop the ongoing recording
            recorder.stopRecording().fold(
                onSuccess = { recordingFile ->
                    Log.d(TAG, "STOPWATCH STOPPED")
                    stopWatch.stop()
                    updateRecordingToExternalStorage(recordingFile).also { transferResult ->
                        if (transferResult.isFailure) {
                            recorder.discardRecordingFile(recordingFile)
                        }
                    }
                },
                onFailure = { Result.failure(it) },
            )
        }
    }

    override suspend fun pauseRecording() {
        _lock.tryWithLock(this) {
            try {
                val recorder = _recorder ?: throw IllegalStateException("MediaRecorder could not be created")
                Log.d(TAG, "STOPWATCH PAUSED")
                recorder.pauseRecording().getOrThrow()
                stopWatch.pause()
                Log.d(TAG, "RECORDER PAUSED")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "FAILED TO PAUSE RECORDING", e)
            }
        }
    }

    override suspend fun resumeRecording() {
        _lock.tryWithLock(this) {
            try {
                val recorder = _recorder ?: throw IllegalStateException("MediaRecorder could not be created")
                //resume recorder
                recorder.resumeRecording()
                Log.d(TAG, "STOPWATCH RESUMED")
                stopWatch.startOrResume()
                Log.d(TAG, "RECORDER RESUMED")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "FAILED TO RESUME RECORDING", e)
            }
        }
    }

    override suspend fun cancelRecording() {
        // if it's holding the lock don't do anything
        _lock.tryWithLock(this) {
            try {
                val recorder = _recorder ?: throw IllegalStateException("MediaRecorder could not be created")
                // cancel the timer watch
                Log.d(TAG, "STOPWATCH STOPPED")
                stopWatch.cancel()
                //stop the ongoing recording
                Log.d(TAG, "RECORDER STOPPED")
                recorder.cancelOngoingRecording()
                Log.d(TAG, "RECORDER STOPPED")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "FAILED TO CANCEL RECORDING", e)
            } finally {
                pcmReader.releaseRecorder()
            }
        }
    }

    override fun releaseResources() {
        try {
            Log.d(TAG, "RELEASING RECORDER")
            _recorder?.release()
        } catch (e: Exception) {
            Log.w(TAG, "FAILED TO RELEASE RESOURCES ", e)
        } finally {
            Log.d(TAG, "CLEANING THE RECORDER")
            // clear the scope
            _scope.cancel()
            //set buffer reader to null
            pcmReader.releaseRecorder()
            // resetting the stopwatch
            Log.d(TAG, "RESETTING STOPWATCH")
            stopWatch.reset()
        }
    }

    private suspend fun setupRecorder(): GenericVoiceRecorder {
        val settings = settings.audioSettings()
        val encoder = settings.encoders
        return when (encoder) {
            RecordingEncoders.ACC, RecordingEncoders.OPTUS, RecordingEncoders.AMR_NB, RecordingEncoders.AMR_WB -> androidRecorder
            RecordingEncoders.WAV, RecordingEncoders.FLAC -> customRecorder
        }.apply { _recorder = this }
    }
}
