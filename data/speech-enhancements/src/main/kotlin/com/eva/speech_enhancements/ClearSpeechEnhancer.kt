package com.eva.speech_enhancements

import android.util.Log
import com.eva.speech_enhancements.exception.SpeechEnhancementHandleException
import com.eva.speech_enhancements.exception.SpeechEnhancerStateException
import com.eva.speech_enhancements.exception.SpeechNativeException
import com.eva.speech_enhancements.models.EnhanceOptions
import com.eva.speech_enhancements.models.SampleBuffer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndUpdate
import kotlin.concurrent.withLock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@OptIn(ExperimentalAtomicApi::class)
class ClearSpeechEnhancer private constructor(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : IClearSpeechEnhancer {

    constructor(
        modelDir: File,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) : this(dispatcher) {
        create(modelDir)
    }

    private val _nativeHandle = AtomicLong(0L)
    private val _isInitialized = AtomicBoolean(false)
    private val _isPrepared = AtomicBoolean(false)
    private val _isFinished = AtomicBoolean(false)

    private val _callLocks = ReentrantLock()
    private val _streamLocks = Mutex()

    private val _pending = SampleBuffer()

    @kotlin.concurrent.Volatile
    private var _droppedLeading = 0L

    @kotlin.concurrent.Volatile
    private var _fedSamples = 0L

    @kotlin.concurrent.Volatile
    private var _returnedSamples = 0L

    override val latencySamples: Int
        get() = withNativeHandle(::nativeLatencySamples)

    override val isReady: Boolean
        get() = _isPrepared.load()

    private fun create(modelDir: File): Unit = _callLocks.withLock {
        if (!modelDir.exists()) throw SpeechNativeException("Model directory not found")

        // a check to ensure the onnx and ini file exists
        val files = modelDir.listFiles()?.filterNotNull() ?: emptyList()
        val hasOnnxFile = files.any { it.extension == "onnx" }
        val hasIniFile = files.any { it.extension == "ini" }

        if (!hasOnnxFile || !hasIniFile) throw IllegalArgumentException("Invalid files dir")

        val handle = createNative(modelDir.absolutePath)
        if (handle == 0L)
            throw SpeechNativeException("Failed to create the speech enhancement native instance")

        val isReady = _isInitialized.compareAndSet(expectedValue = false, newValue = true)

        if (!isReady) {
            destroyNative(handle)
            throw SpeechNativeException("Speech enhancer was initialized on another thread")
        }
        _nativeHandle.store(handle)
        Log.i(TAG, "SPEECH ENHANCER INSTANCE CREATED")
    }


    override suspend fun prepare(): Unit = withContext(dispatcher) {
        withNativeHandle { handle ->
            val isSuccess = prepareNative(handle)
            if (!isSuccess) throw SpeechNativeException("Failed to prepare the model")
        }
        _isPrepared.store(true)

    }

    override suspend fun process(chunk: FloatArray, options: EnhanceOptions): FloatArray = withContext(dispatcher) {
        if (chunk.isEmpty()) return@withContext FloatArray(0)
        _streamLocks.withLock {

            isReadyToStream()

            val buffer = withNativeHandle { handle ->
                try {
                    processNative(
                        handle = handle,
                        samples = chunk.toNativeByteBuffer(),
                        strength = options.blending.strength,
                        adaptive = options.blending.adaptive,
                        postFilterBeta = options.tuning.postFilterBeta,
                        attenuationLimitDb = options.tuning.attenuationLimitDb,
                    )
                } catch (e: SpeechNativeException) {
                    throw e
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    throw SpeechNativeException(e.message ?: "Enhancement failed on the native layer")
                }
            }

            if (buffer == null) throw SpeechNativeException("Enhancement failed, results are null")

            _fedSamples += chunk.size

            accumulateEmitted(buffer.toFloatArray())
            dropLeadingLatency()
            takeAligned()
        }
    }

    override suspend fun finish(options: EnhanceOptions): FloatArray = withContext(dispatcher) {
        _streamLocks.withLock {
            isReadyToStream()

            val buffer = withNativeHandle { handle ->
                try {
                    finishNative(
                        handle = handle,
                        strength = options.blending.strength,
                        adaptive = options.blending.adaptive,
                        postFilterBeta = options.tuning.postFilterBeta,
                        attenuationLimitDb = options.tuning.attenuationLimitDb,
                    )
                } catch (e: SpeechNativeException) {
                    throw e
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    throw SpeechNativeException(e.message ?: "Enhancement failed on the native layer")
                }
            }

            if (buffer == null) throw SpeechNativeException("Enhancement failed, results are null")

            accumulateEmitted(buffer.toFloatArray())
            dropLeadingLatency()
            val tail = takeAligned()
            _isFinished.compareAndSet(expectedValue = false, true)
            Log.i(TAG, "SPEECH ENHANCEMENT STREAM FINISHED")
            tail
        }
    }

    override suspend fun reset(): Unit = withContext(dispatcher) {
        _streamLocks.withLock {
            withNativeHandle { handle -> resetNative(handle) }
            clearStreamState()
            Log.i(TAG, "SPEECH ENHANCER INSTANCE RESET")
        }
    }

    override fun close() = _callLocks.withLock {
        if (!_isInitialized.compareAndSet(expectedValue = true, newValue = false)) return@withLock
        _isPrepared.store(false)
        clearStreamState()
        val handle = _nativeHandle.fetchAndUpdate { 0L }
        if (handle != 0L) destroyNative(handle)
    }


    private fun accumulateEmitted(emitted: FloatArray) = _pending.append(emitted)

    private fun dropLeadingLatency() {
        val droppable = (latencySamples - _droppedLeading).coerceAtLeast(0L)
        val drop = minOf(droppable, _pending.size.toLong())
        _droppedLeading += drop
        _pending.dropFront(drop.toInt())
    }

    private fun takeAligned(): FloatArray {
        val returnable = (_fedSamples - _returnedSamples).coerceAtLeast(0L)
        val count = minOf(_pending.size.toLong(), returnable).toInt()
        _returnedSamples += count
        return _pending.takeFront(count)
    }

    private fun clearStreamState() {
        _pending.clear()
        _droppedLeading = 0L
        _fedSamples = 0L
        _returnedSamples = 0L
        _isFinished.store(false)
    }

    private fun isReadyToStream() {
        if (_nativeHandle.load() == 0L || !_isInitialized.load()) throw SpeechEnhancementHandleException()
        if (!_isPrepared.load()) throw SpeechEnhancerStateException("Speech enhancer is not prepared")
        if (_isFinished.load()) throw SpeechEnhancerStateException("The stream is already finished; call reset() to start a new one")
    }

    private fun <T> withNativeHandle(lambda: (Long) -> T): T = _callLocks.withLock {
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

    private external fun processNative(
        handle: Long,
        samples: ByteBuffer,
        strength: Float,
        adaptive: Boolean,
        postFilterBeta: Float,
        attenuationLimitDb: Float,
    ): ByteBuffer?

    private external fun finishNative(
        handle: Long,
        strength: Float,
        adaptive: Boolean,
        postFilterBeta: Float,
        attenuationLimitDb: Float,
    ): ByteBuffer?

    private external fun nativeLatencySamples(handle: Long): Int

    companion object {
        private const val TAG = "ClearSpeechEnhancer"

        init {
            System.loadLibrary("speech_enhancement")
        }
    }
}
