package com.eva.recorder_native

import android.util.Log
import com.eva.recorder_native.exception.InvalidHandleResultException
import com.eva.recorder_native.exception.RecorderAlreadyInitiatedException
import com.eva.recorder_native.exception.RecorderHandleException
import com.eva.recorder_native.models.RecorderConfig
import java.io.File
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndUpdate

@OptIn(ExperimentalAtomicApi::class)
class WavAudioRecorder : CustomRecorder {

    private val _nativeHandle = AtomicLong(0L)
    private val _isInitialized = AtomicBoolean(false)

    private val _lock = Any()

    @Volatile
    override var onError: ((Throwable) -> Unit)? = null

    override fun create() = synchronized(_lock) {
        if (_isInitialized.load()) throw RecorderAlreadyInitiatedException()

        val handle = try {
            createRecorder()
        } catch (e: Throwable) {
            notifyError(e)
            throw e
        }

        if (handle == 0L) throw InvalidHandleResultException()

        if (!_isInitialized.compareAndSet(expectedValue = false, newValue = true)) {
            releaseNative(handle)
            throw IllegalStateException("Recorder was init on another thread")
        }
        Log.i(TAG, "RECORDER INSTANCE CREATED")
        _nativeHandle.store(handle)
    }

    override fun release() = synchronized(_lock) {
        if (!_isInitialized.compareAndSet(expectedValue = true, newValue = false)) {
            Log.w(TAG, "RECORDER WAS NOT CREATED")
            return@synchronized
        }
        val handle = _nativeHandle.fetchAndUpdate { 0L }
        if (handle != 0L) {
            try {
                releaseNative(handle)
            } catch (e: Throwable) {
                notifyError(e)
                throw e
            }
            Log.i(TAG, "RECORDER INSTANCE RELEASED")
        }
    }

    override val isRecording: Boolean
        get() = withNativeHandle(::isRecordRunning)

    override val droppedFrames: Long
        get() = withNativeHandle(::getDroppedFrames)

    override fun getReadRecorderConfig(): RecorderConfig? = synchronized(_lock) {
        if (!_isInitialized.load()) return@synchronized null
        withNativeHandle { handle ->
            getRecorderConfig(handle)?.let { RecorderConfig(sampleRate = it[0], channel = it[1]) }
        }
    }

    override fun setOutputFile(file: File) = withNativeHandle { setOutputFileConfig(it, file.absolutePath) }

    override fun setRecorderConfig(config: RecorderConfig) =
        withNativeHandle { setConfig(it, config.sampleRate, config.channel) }

    override fun prepare() = withNativeHandle(::prepareRecorder)

    override fun start() = withNativeHandle(::startRecorder)

    override fun stop() = withNativeHandle(::stopRecorder)

    override fun pause() = withNativeHandle(::pauseRecorder)

    override fun resume() = withNativeHandle(::resumeRecorder)

    override fun reset() = withNativeHandle(::resetRecorder)

    private inline fun <T> withNativeHandle(block: (Long) -> T): T = synchronized(_lock) {
        if (!_isInitialized.load()) {
            Log.w(TAG, "RECORDER WAS NOT INITIATED")
            throw RecorderHandleException()
        }
        val handle = _nativeHandle.load().takeIf { it != 0L } ?: throw RecorderHandleException()
        try {
            block(handle)
        } catch (error: Throwable) {
            notifyError(error)
            throw error
        }
    }

    private fun notifyError(error: Throwable) {
        try {
            onError?.invoke(error)
        } catch (handlerError: Throwable) {
            Log.e(TAG, "Recorder error handler failed", handlerError)
        }
    }

    // create and release
    private external fun createRecorder(): Long
    private external fun releaseNative(handle: Long)

    // audio and file configuration
    private external fun setConfig(handle: Long, sampleRate: Int, channels: Int)
    private external fun setOutputFileConfig(handle: Long, filePath: String)

    // lifecycle
    private external fun prepareRecorder(handle: Long)
    private external fun startRecorder(handle: Long)
    private external fun stopRecorder(handle: Long)
    private external fun pauseRecorder(handle: Long)
    private external fun resumeRecorder(handle: Long)
    private external fun resetRecorder(handle: Long)

    // helpers
    private external fun isRecordRunning(handle: Long): Boolean
    private external fun getDroppedFrames(handle: Long): Long
    private external fun getRecorderConfig(handle: Long): IntArray?

    companion object {

        private const val TAG = "WavAudioRecorder"

        init {
            System.loadLibrary("wav_recorder")
        }
    }
}
