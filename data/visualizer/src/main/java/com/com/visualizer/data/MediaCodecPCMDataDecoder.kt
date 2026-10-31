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
import com.com.visualizer.domain.exception.InvalidMimeTypeException
import com.com.visualizer.domain.exception.MediaExtractorException
import com.com.visualizer.utils.isThreadAlive
import com.com.visualizer.utils.safePOST
import java.nio.ByteOrder
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
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
    private var _onDecodeComplete: (suspend () -> Unit)? = null

    // number of points buffered before a batch is emitted, sized per run
    // from the length of the track
    private var _batchSize = 1

    // accumulates the RMS of each decoded window before emitting a batch
    private var _batch = FloatArray(1)
    private var _batchedPoints = 0

    // duration of a single visualization window in microseconds
    private val windowUs = seekDurationMillis * 1_000L

    // index of the output window currently being accumulated, -1 until the
    // first decoded buffer arrives
    private var _currentWindowIndex = -1

    // energy and decoded frame count of the window being accumulated
    private var _windowSquares = 0.0
    private var _windowFrames = 0

    // time constraints
    private val _totalTimeInMs = AtomicLong(0L)

    // flags
    private var _isCleaningUp = AtomicBoolean(false)

    override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
        // clean up is called thus no more processing
        if (_isCleaningUp.load()) {
            Log.i(CODEC_TAG, "IGNORING READING INPUT CALLBACK ")
            return
        }
        try {
            // codec state is non exec means work is done return END_OF_STREAM
            if (_codecState != MediaCodecState.EXEC) {
                Log.w(PROCESSING_TAG, "WRONG CODEC STATE")
                codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                return
            }
        } catch (e: Exception) {
            Log.e(PROCESSING_TAG, "UNABLE TO SEND EOS FLAG", e)
        }

        try {
            // receive a buffer
            if (index < 0) return
            val inputBuffer = codec.getInputBuffer(index) ?: return

            val extractor = _extractor ?: run {
                codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                Log.w(EXTRACTOR_TAG, "EXTRACTOR RELEASED CANNOT CALCULATE")
                return
            }

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

            // do not read past the known duration of the track
            val totalUs = _totalTimeInMs.load() * 1_000L
            if (totalUs > 0 && sampleTimeUs >= totalUs + windowUs) {
                codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                Log.i(PROCESSING_TAG, "TOTAL TIME HAS REACHED SENDING END OF STREAM")
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
        } catch (e: IllegalStateException) {
            Log.e(PROCESSING_TAG, "MEDIA CODEC IS NOT IN EXECUTION STATE", e)
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
            Log.w(PROCESSING_TAG, "WRONG STATE SHOULD BE EXEC FOUND :$_codecState")
            return
        }
        // special case to handle end of stream
        if (info.isEndOfStream) {
            codec.stop()
            _codecState = MediaCodecState.RELEASED
            handleEndOfStream()
            return
        }
        if (info.size > 0) {
            try {
                if (_codecState != MediaCodecState.EXEC) return
                val outputBuffer = codec.getOutputBuffer(index) ?: return
                outputBuffer.order(ByteOrder.LITTLE_ENDIAN)

                val format = codec.outputFormat
                val pcmEncoding = format.pcmEncoding
                val channelCount = format.channels

                // decoded PCM is bucketed into fixed windows of the
                // presentation time, a point is emitted for each completed
                // window, buffers straddling a boundary count towards the
                // window their presentation time falls into
                val windowIndex = (info.presentationTimeUs / windowUs).toInt()
                if (windowIndex != _currentWindowIndex) {
                    emitWindowPoint()
                    _currentWindowIndex = windowIndex
                }

                // energy of this buffer accumulates into the running window,
                // no intermediate arrays, boxing or thread dispatches
                _windowFrames += pcmFrameCount(info.size, pcmEncoding, channelCount)
                _windowSquares += outputBuffer.sumOfSquaredMonoFrames(info.size, pcmEncoding, channelCount)

                // release the buffer as work is done
                if (_codecState == MediaCodecState.EXEC)
                    codec.releaseOutputBuffer(index, false)
            } catch (e: IllegalStateException) {
                Log.e(PROCESSING_TAG, "WRONG STATE TO HANDLE THE OUTPUT BUFFERS", e)
            }
        }
    }

    override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
        if (e.isRecoverable) codec.stop()

        Log.e(CODEC_TAG, "ERROR HAPPENED", e)
    }

    override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
        if (format.keys.contains(MediaFormat.KEY_DURATION)) {
            _totalTimeInMs.store(format.duration?.inWholeMilliseconds ?: 0L)
        }
        Log.d(CODEC_TAG, "MEDIA FORMAT CHANGED: KEYS:${format.keys}")
    }

    /**
     * Emits the root-mean-square of the window that was being accumulated
     * and resets the accumulator, windows that saw no decoded audio are
     * dropped instead of emitting a silence point.
     */
    private fun emitWindowPoint() {
        if (_windowFrames > 0) {
            _batch[_batchedPoints++] = sqrt(_windowSquares / _windowFrames).toFloat()

            // emit the points in batches to the visualizer
            if (_batchedPoints == _batchSize) flushBatch()
        }
        _windowSquares = 0.0
        _windowFrames = 0
    }

    private fun flushBatch() {
        if (_batchedPoints == 0) return
        _onBufferDecoded?.invoke(_batch.copyOf(_batchedPoints))
        _batchedPoints = 0
    }

    private fun handleEndOfStream() {
        Log.d(PROCESSING_TAG, "END OF BUFFER REACHED, FLUSHING LEFT OUT POINTS")
        // emit the window the stream ended in, it is shorter than a full one
        emitWindowPoint()
        // emit whatever is left before completing
        flushBatch()
        // stream read ended
        runBlocking { _onDecodeComplete?.invoke() }
    }

    fun setOnBufferDecode(listener: (FloatArray) -> Unit) {
        _onBufferDecoded = listener
    }

    fun setOnComplete(listener: suspend () -> Unit) {
        _onDecodeComplete = listener
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
        _currentWindowIndex = -1
        _windowSquares = 0.0
        _windowFrames = 0

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
        if (!isAlive) return

        // codec is configured
        _mediaCodec = _codecList.findDecoderForFormat(format)?.let { codecName ->
            Log.i(CODEC_TAG, "CONFIGURING CODEC BY NAME :$codecName")
            MediaCodec.createByCodecName(codecName)
        } ?: run {
            val mimeType = format.getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm"
            Log.i(CODEC_TAG, "CONFIGURING CODEC BY DECODER TYPE :$mimeType ")
            MediaCodec.createDecoderByType(mimeType)
        }

        _mediaCodec?.setCallback(this@MediaCodecPCMDataDecoder, handler)
        _mediaCodec?.configure(format, null, null, 0)

        Log.i(CODEC_TAG, "MEDIA CODEC CONFIGURED THREAD:${handler.looper.thread}")
        Log.d(CODEC_TAG, "MEDIA CODEC NAME :${_mediaCodec?.name}")

        val caller = Runnable {
            _mediaCodec?.start()
            _codecState = MediaCodecState.EXEC
            Log.i(CODEC_TAG, "MEDIA CODEC STARTED ON :${Thread.currentThread()}")
        }
        // start the codec
        if (handler.looper == Looper.getMainLooper()) caller.run()
        else {
            val isPosted = handler.post(caller)
            if (!isPosted) caller.run()
        }
    }


    @Synchronized
    fun cleanUp() {
        val caller = Runnable { codecClean() }

        // when called from the codec thread run inline, a posted cleanup could
        // otherwise be dropped by the thread controller which clears the
        // handler queue before quitting the looper
        if (handler == null || handler.looper == Looper.getMainLooper() ||
            Looper.myLooper() == handler.looper
        ) {
            caller.run()
        } else {
            val isPosted = handler.safePOST(caller)
            if (!isPosted) caller.run()
        }
    }

    private fun codecClean() {
        try {
            Log.i(CODEC_TAG, "CLEANING UP IN :${Thread.currentThread()}")
            _isCleaningUp.compareAndSet(expectedValue = false, newValue = true)
            if (_codecState == MediaCodecState.EXEC) {
                // flush all the buffers
                _mediaCodec?.flush()
                // codec is stopped then switched to stopped state
                _mediaCodec?.stop()
                _codecState = MediaCodecState.STOPPED
            }
        } catch (e: Exception) {
            Log.e(CODEC_TAG, "UNABLE TO STOP CODEC", e)
        } finally {
            _isCleaningUp.compareAndSet(expectedValue = true, newValue = false)
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

        // release the codec
        try {
            _mediaCodec?.release()
            Log.i(CODEC_TAG, "MEDIA CODEC RELEASED")
        } finally {
            _codecState = MediaCodecState.RELEASED
            _mediaCodec = null
        }

        // release the extractor
        _extractor?.release()
        Log.d(EXTRACTOR_TAG, "EXTRACTOR RELEASED")
    }

    companion object {
        // a batch covers this much audio for regular length recordings
        private val normalBatchDuration = 10.seconds
        private val longBatchDuration = 20.seconds

        // long batch min milliseconds
        private val longRecordingDuration = 10.minutes
    }
}
