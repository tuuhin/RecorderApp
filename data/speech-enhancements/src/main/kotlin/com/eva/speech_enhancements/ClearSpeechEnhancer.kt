package com.eva.speech_enhancements

import android.util.Log
import com.eva.speech_enhancements.exception.SpeechEnhancementHandleException
import com.eva.speech_enhancements.exception.SpeechEnhancerStateException
import com.eva.speech_enhancements.exception.SpeechNativeException
import com.eva.speech_enhancements.models.EnhanceAudioResult
import com.eva.speech_enhancements.models.EnhanceOptions
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndUpdate
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalAtomicApi::class)
class ClearSpeechEnhancer(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : IClearSpeechEnhancer {

    private val _nativeHandle = AtomicLong(0L)

    private val _isInitialized = AtomicBoolean(false)
    private val _isSamplesConsumed = AtomicBoolean(false)

    private val _lock = Any()

    override val latencySamples: Int
        get() = withNativeHandle(::nativeLatencySamples)

    override fun create(modelDir: File) = synchronized(_lock) {

        if (_isInitialized.load())
            throw SpeechNativeException("Speech enhancer instance is already present, release it first")

        if (!modelDir.exists()) throw SpeechNativeException("Model directory not found")

        val files = modelDir.listFiles()?.filterNotNull() ?: emptyList()
        val hasOnnxFile = files.any { it.extension == "onnx" }
        val hasIniFile = files.any { it.extension == "ini" }

        if (!hasOnnxFile || !hasIniFile) throw IllegalArgumentException("Invalid files dir")

        val handle = createNative(modelDir.absolutePath)
        if (handle == 0L)
            throw SpeechNativeException("Failed to create the speech enhancement native instance")

        if (!_isInitialized.compareAndSet(expectedValue = false, newValue = true)) {
            destroyNative(handle)
            throw SpeechNativeException("Speech enhancer was initialized on another thread")
        }
        _isSamplesConsumed.store(false)
        Log.i(TAG, "SPEECH ENHANCER INSTANCE CREATED")
        _nativeHandle.store(handle)
    }

    override suspend fun prepare(): Unit = withContext(dispatcher) {
        withNativeHandle { handle ->
            if (prepareNative(handle)) return@withNativeHandle
            throw SpeechNativeException("Model files are missing or unreadable; prepare a streaming model directory first")
        }
    }

    override suspend fun reset(): Boolean = withContext(dispatcher) {
        withNativeHandle { handle ->
            val didReset = resetNative(handle)
            // the stream is clean again either way; the flag must not survive a reset
            _isSamplesConsumed.store(false)
            Log.i(TAG, "SPEECH ENHANCER INSTANCE RESET: $didReset")
            didReset
        }
    }

    override suspend fun enhance(
        pcmSamples: FloatArray,
        sampleRate: Int,
        options: EnhanceOptions,
    ): EnhanceAudioResult = withContext(dispatcher) {

        if (pcmSamples.isEmpty()) throw IllegalArgumentException("samples must not be empty")
        if (sampleRate != 48_000) throw IllegalArgumentException("the native engine processes 48 kHz input only")

        val startedAt = System.nanoTime()

        val enhanced = withNativeHandle { handle ->
            try {
                if (!_isSamplesConsumed.compareAndSet(expectedValue = false, newValue = true))
                    throw SpeechEnhancerStateException()

                // call enhancements if samples consumed was false
                enhanceNative(handle, pcmSamples.toNativeByteBuffer(), sampleRate, options.strength)
            } catch (e: Throwable) {
                // a failed run did not consume the stream; the call stays retryable
                _isSamplesConsumed.store(false)
                throw e
            }
        }

        if (enhanced == null) {
            _isSamplesConsumed.store(false)
            throw SpeechNativeException("Enhancement failed, results are null")
        }


        val floatResults = enhanced.toFloatArray()

        val processingDuration = (System.nanoTime() - startedAt).nanoseconds
        val totalDuration = (floatResults.size.toDouble() / sampleRate).seconds

        EnhanceAudioResult(
            samples = floatResults,
            sampleRate = 48_000,
            totalDuration = totalDuration,
            processingDuration = processingDuration,
        )
    }

    override fun close() = synchronized(_lock) {
        if (!_isInitialized.compareAndSet(expectedValue = true, newValue = false)) return@synchronized
        val handle = _nativeHandle.fetchAndUpdate { 0L }
        if (handle != 0L) destroyNative(handle)
    }

    private fun <T> withNativeHandle(lambda: (Long) -> T): T = synchronized(_lock) {
        val handle = _nativeHandle.load().takeIf { it != 0L }
        if (handle == null || !_isInitialized.load()) {
            Log.w(TAG, "ENHANCER INSTANCE WAS NOT CREATED OR WAS ALREADY RELEASED")
            throw SpeechEnhancementHandleException()
        }
        try {
            lambda(handle)
        } catch (e: Exception) {
            Log.e(TAG, "FAILED TO CALL THE NATIVE LAYER", e)
            throw e
        }
    }

    private fun FloatArray.toNativeByteBuffer(): ByteBuffer {
        val buffer = ByteBuffer
            .allocateDirect(size * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
        buffer.asFloatBuffer().put(this)
        buffer.rewind()
        return buffer
    }

    private fun ByteBuffer.toFloatArray(): FloatArray {
        val dup = duplicate().order(ByteOrder.nativeOrder())
        if (dup.position() != 0) dup.flip()
        val floatBuf = dup.asFloatBuffer()
        val out = FloatArray(floatBuf.remaining())
        floatBuf.get(out)
        return out
    }

    private external fun createNative(modelDir: String): Long
    private external fun destroyNative(handle: Long)
    private external fun prepareNative(handle: Long): Boolean
    private external fun resetNative(handle: Long): Boolean

    private external fun enhanceNative(
        handle: Long,
        samples: ByteBuffer,
        sampleRate: Int,
        strength: Float,
    ): ByteBuffer?

    private external fun nativeLatencySamples(handle: Long): Int

    companion object {
        private const val TAG = "ClearSpeechEnhancer"

        init {
            System.loadLibrary("speech_enhancement")
        }
    }
}
