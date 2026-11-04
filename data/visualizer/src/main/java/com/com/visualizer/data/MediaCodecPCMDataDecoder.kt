package com.com.visualizer.data

import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaFormat
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.inspector.MediaExtractorCompat
import com.com.visualizer.data.ext.MediaCodecState
import com.com.visualizer.data.ext.channels
import com.com.visualizer.data.ext.duration
import com.com.visualizer.data.ext.isEndOfStream
import com.com.visualizer.data.ext.mimeType
import com.com.visualizer.data.ext.pcmEncoding
import com.com.visualizer.data.ext.sampleRate
import com.com.visualizer.domain.exception.InvalidMimeTypeException
import com.com.visualizer.domain.exception.MediaExtractorException
import com.com.visualizer.utils.isThreadAlive
import java.nio.ByteBuffer
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

private const val CODEC_TAG = "CODEC_CALLBACK"
private const val PROCESSING_TAG = "CODEC_PROCESSING"
private const val EXTRACTOR_TAG = "MEDIA_EXTRACTOR"

@UnstableApi
@OptIn(ExperimentalAtomicApi::class)
internal class MediaCodecPCMDataDecoder(
    private val seekDurationMillis: Int,
    private val handler: Handler? = null,
) : MediaCodec.Callback() {

    private val ioDispatcher = Dispatchers.IO.limitedParallelism(1)

    @Volatile
    private var _mediaCodec: MediaCodec? = null

    @Volatile
    private var _extractor: MediaExtractorCompat? = null

    @Volatile
    private var _codecState = MediaCodecState.RELEASED

    private val _codecList by lazy { MediaCodecList(MediaCodecList.REGULAR_CODECS) }

    // callbacks
    private var _onBufferDecoded: ((FloatArray) -> Unit)? = null
    private var _onDecodeComplete: (() -> Unit)? = null
    private var _onDecodeFailure: ((Throwable) -> Unit)? = null

    // number of points buffered before a batch is emitted, sized per run
    // from the length of the track
    private var _batchSize = 1

    // accumulates the RMS of each decoded window before emitting a batch
    private var _batch = FloatArray(1)
    private var _batchedPoints = 0

    private val _accumulator = PcmWindowAccumulator(seekDurationMillis, ::emitWindowPoint)

    // time constraints
    private val _totalTimeInMs = AtomicLong(0L)

    // flags
    private var _isCleaningUp = AtomicBoolean(false)
    private val _failureReported = AtomicBoolean(false)

    override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
        // clean up is called thus no more processing
        if (_isCleaningUp.load()) {
            Log.i(CODEC_TAG, "IGNORING READING INPUT CALLBACK ")
            return
        }
        if (_codecState != MediaCodecState.EXEC) {
            reportFailure(IllegalStateException("Codec is not executing"))
            return
        }

        try {
            // receive a buffer
            if (index < 0) return
            val inputBuffer = checkNotNull(codec.getInputBuffer(index)) { "Codec input buffer is unavailable" }
            val extractor = checkNotNull(_extractor) { "Audio extractor is unavailable" }

            // Feed every sample of the stream: compressed frames are not
            // self-contained, mp3 and aac borrow bytes of earlier frames
            // through the bit reservoir, so frames cannot be skipped on the
            // input side without breaking to decode. The visualization
            // windows are cut from the decoded output instead.
            val sampleTimeUs = extractor.sampleTime

            // no more samples left in the stream
            if (sampleTimeUs < 0) {
                codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                Log.i(PROCESSING_TAG, "INPUT BUFFER :END OF INPUT STREAM")
                return
            }

            val sampleSize = extractor.readSampleData(inputBuffer, 0)

            // sample size is zero thus processing done END_OF_STREAM
            if (sampleSize <= 0) {
                codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                Log.i(PROCESSING_TAG, "INPUT BUFFER :END OF INPUT STREAM")
                return
            }

            // advance the extractor for the next callback, the sample time
            // was read before advancing so it belongs to the fed sample
            extractor.advance()

            codec.queueInputBuffer(index, 0, sampleSize, sampleTimeUs, 0)
        } catch (e: MediaCodec.CodecException) {
            Log.e(PROCESSING_TAG, "MEDIA CODEC EXCEPTION ${e.diagnosticInfo} ${e.errorCode} ", e)
            reportFailure(e)
        } catch (e: IllegalStateException) {
            Log.e(PROCESSING_TAG, "MEDIA CODEC IS NOT IN EXECUTION STATE", e)
            reportFailure(e)
        } catch (e: Exception) {
            Log.e(PROCESSING_TAG, "UNABLE TO READ AUDIO INPUT", e)
            reportFailure(e)
        }
    }


    override fun onOutputBufferAvailable(
        codec: MediaCodec,
        index: Int,
        info: MediaCodec.BufferInfo,
    ) {
        // look is this cleaning up
        if (_isCleaningUp.load()) {
            Log.i(CODEC_TAG, "IGNORING PROCESSING OUTPUT CALLBACK")
            return
        }
        // correct state or not
        if (_codecState != MediaCodecState.EXEC) {
            reportFailure(IllegalStateException("Codec output arrived while not executing"))
            return
        }
        try {
            val outputBuffer = if (info.size > 0) codec.getOutputBuffer(index) else null
            val format = if (info.size > 0) codec.outputFormat else null
            // consume the accumulator codec output
            _accumulator.consumeCodecOutput(outputBuffer, info, format)
            // clear the buffer
            codec.releaseOutputBuffer(index, false)
            if (info.isEndOfStream) {
                _isCleaningUp.store(true)
                handleEndOfStream()
            }
        } catch (e: Exception) {
            Log.e(PROCESSING_TAG, "UNABLE TO HANDLE THE OUTPUT BUFFER", e)
            reportFailure(e)
        }
    }

    override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
        if (_isCleaningUp.load()) return
        Log.e(CODEC_TAG, "ERROR HAPPENED", e)
        reportFailure(e)
    }

    override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
        if (format.keys.contains(MediaFormat.KEY_DURATION)) {
            _totalTimeInMs.store(format.duration?.inWholeMilliseconds ?: 0L)
        }
        Log.d(CODEC_TAG, "MEDIA FORMAT CHANGED: KEYS:${format.keys}")
    }

    private fun emitWindowPoint(value: Float) {
        _batch[_batchedPoints++] = value
        if (_batchedPoints == _batchSize) flushBatch()
    }

    private fun flushBatch() {
        if (_batchedPoints == 0) return
        _onBufferDecoded?.invoke(_batch.copyOf(_batchedPoints))
        _batchedPoints = 0
    }

    private fun handleEndOfStream() {
        Log.d(PROCESSING_TAG, "END OF BUFFER REACHED, FLUSHING LEFT OUT POINTS")
        flushBatch()
        _onDecodeComplete?.invoke()
    }

    fun setOnBufferDecode(listener: (FloatArray) -> Unit) {
        _onBufferDecoded = listener
    }

    fun setOnComplete(listener: () -> Unit) {
        _onDecodeComplete = listener
    }

    fun setOnFailure(listener: (Throwable) -> Unit) {
        _onDecodeFailure = listener
    }

    suspend fun initiateExtraction(extractor: ExtractorsFactory, dataSource: DataSource.Factory, fileURI: Uri)
        : Result<Unit> = withContext(ioDispatcher) {

        _extractor?.release()
        _extractor = MediaExtractorCompat(extractor, dataSource).apply {
            setDataSource(fileURI, 0)
        }
        // check track count
        if (_extractor?.trackCount == 0)
            return@withContext Result.failure(MediaExtractorException("No track found"))

        val format = _extractor!!.getTrackFormat(0)
        val mimeType = format.mimeType

        // check audio duration
        if (!format.keys.contains(MediaFormat.KEY_DURATION))
            return@withContext Result.failure(MediaExtractorException("Cannot determine track duration"))

        // check mime type
        if (mimeType == null || !mimeType.startsWith("audio"))
            return@withContext Result.failure(InvalidMimeTypeException())

        // set the total time
        _totalTimeInMs.store(format.duration?.inWholeMilliseconds ?: 0L)

        Log.i(EXTRACTOR_TAG, "EXTRACTOR PREPARED")
        _extractor?.selectTrack(0)
        initiateCodec(format)
        Result.success(Unit)
    }


    private fun initiateCodec(format: MediaFormat) {
        _isCleaningUp.store(false)
        _failureReported.store(false)

        // determines the batch size based on the total time of the clip
        val batchMs = if (_totalTimeInMs.load() > longRecordingDuration.inWholeMilliseconds)
            longBatchDuration.inWholeMilliseconds
        else normalBatchDuration.inWholeMilliseconds

        _batchSize = (batchMs / seekDurationMillis).toInt().coerceAtLeast(1)
        if (_batch.size < _batchSize) _batch = FloatArray(_batchSize)
        _batchedPoints = 0
        Log.i(PROCESSING_TAG, "EMITTING $_batchSize POINTS PER BATCH COVERING ${batchMs / 1000}S OF AUDIO")

        if (_codecState == MediaCodecState.RELEASED) Log.d(CODEC_TAG, "FRESH INSTANCE FOUND")

        // codec is reset
        _mediaCodec?.reset()
        _codecState = MediaCodecState.STOPPED

        val isAlive = handler?.isThreadAlive() ?: false

        Log.i(CODEC_TAG, "HANDLER THREAD ALIVE :$isAlive")
        check(isAlive) { "The codec handler thread is not alive" }
        val callbackHandler = handler

        // codec is configured
        _mediaCodec = _codecList.findDecoderForFormat(format)?.let { codecName ->
            Log.i(CODEC_TAG, "CONFIGURING CODEC BY NAME :$codecName")
            MediaCodec.createByCodecName(codecName)
        } ?: run {
            val mimeType = format.getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm"
            Log.i(CODEC_TAG, "CONFIGURING CODEC BY DECODER TYPE :$mimeType ")
            MediaCodec.createDecoderByType(mimeType)
        }

        _mediaCodec?.setCallback(this@MediaCodecPCMDataDecoder, callbackHandler)
        _mediaCodec?.configure(format, null, null, 0)

        Log.i(CODEC_TAG, "MEDIA CODEC CONFIGURED THREAD:${callbackHandler.looper.thread}")
        Log.d(CODEC_TAG, "MEDIA CODEC NAME :${_mediaCodec?.name}")

        val caller = Runnable {
            try {
                _mediaCodec?.start()
                _codecState = MediaCodecState.EXEC
                Log.i(CODEC_TAG, "MEDIA CODEC STARTED ON :${Thread.currentThread()}")
            } catch (e: Exception) {
                reportFailure(e)
            }
        }
        // start the codec
        if (callbackHandler.looper == Looper.getMainLooper()) caller.run()
        else {
            check(callbackHandler.post(caller)) { "Unable to start the media codec" }
        }
    }


    suspend fun cleanUp() {
        _isCleaningUp.store(true)
        val callbackHandler = handler

        if (callbackHandler == null || Looper.myLooper() == callbackHandler.looper) {
            codecClean()
            return
        }

        withContext(NonCancellable + ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val cleanup = Runnable {
                    try {
                        codecClean()
                    } finally {
                        continuation.resume(Unit)
                    }
                }
                // post the cleanup on a handler if failed run directly
                if (!callbackHandler.post(cleanup)) cleanup.run()
            }
        }
    }

    private fun codecClean() {
        _isCleaningUp.store(true)
        try {
            Log.i(CODEC_TAG, "CLEANING UP IN :${Thread.currentThread()}")
            if (_codecState == MediaCodecState.EXEC) {
                try {
                    _mediaCodec?.flush()
                } catch (e: Exception) {
                    Log.w(CODEC_TAG, "UNABLE TO FLUSH CODEC", e)
                }
                try {
                    _mediaCodec?.stop()
                } catch (e: Exception) {
                    Log.w(CODEC_TAG, "UNABLE TO STOP CODEC", e)
                } finally {
                    _codecState = MediaCodecState.STOPPED
                }
            }
        } catch (e: Exception) {
            Log.e(CODEC_TAG, "UNABLE TO STOP CODEC", e)
        }

        try {
            if (_codecState == MediaCodecState.STOPPED) {
                // clear the codec
                val clearDuration = measureTime { _mediaCodec?.setCallback(null) }
                // Give it time to process
                Log.d(CODEC_TAG, "CALLBACK CLEAR POSTED TO HANDLER $clearDuration")
            }
        } catch (e: Exception) {
            Log.e(CODEC_TAG, "FAILED TO CLEAR CALLBACK", e)
        }

        try {
            _mediaCodec?.release()
            Log.i(CODEC_TAG, "MEDIA CODEC RELEASED")
        } catch (e: Exception) {
            Log.e(CODEC_TAG, "FAILED TO RELEASE CODEC", e)
        } finally {
            _codecState = MediaCodecState.RELEASED
            _mediaCodec = null
        }

        try {
            _extractor?.release()
            Log.d(EXTRACTOR_TAG, "EXTRACTOR RELEASED")
        } catch (e: Exception) {
            Log.e(EXTRACTOR_TAG, "FAILED TO RELEASE EXTRACTOR", e)
        } finally {
            _extractor = null
        }
    }

    private fun reportFailure(failure: Throwable) {
        if (!_failureReported.compareAndSet(expectedValue = false, newValue = true)) return
        _isCleaningUp.store(true)
        _onDecodeFailure?.invoke(failure)
    }

    companion object {
        // a batch covers this much audio for regular length recordings
        private val normalBatchDuration = 10.seconds
        private val longBatchDuration = 20.seconds

        // long batch min milliseconds
        private val longRecordingDuration = 10.minutes
    }
}

internal fun PcmWindowAccumulator.consumeCodecOutput(
    buffer: ByteBuffer?,
    info: MediaCodec.BufferInfo,
    format: MediaFormat?,
) {
    if (info.size > 0) {
        val output = checkNotNull(buffer) { "Codec output buffer is unavailable" }
        val pcmFormat = checkNotNull(format) { "Codec output format is unavailable" }
        append(
            buffer = output,
            offset = info.offset,
            size = info.size,
            presentationTimeUs = info.presentationTimeUs,
            sampleRate = pcmFormat.sampleRate,
            channelCount = pcmFormat.channels,
            encoding = pcmFormat.pcmEncoding,
        )
    }
    if (info.isEndOfStream) finish(info.presentationTimeUs)
}
