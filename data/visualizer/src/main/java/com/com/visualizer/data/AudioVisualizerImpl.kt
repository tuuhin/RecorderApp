package com.com.visualizer.data

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ExtractorsFactory
import com.com.visualizer.domain.AudioVisualizer
import com.com.visualizer.domain.ThreadController
import com.com.visualizer.domain.VisualizerState
import com.com.visualizer.domain.exception.DecoderExistsException
import com.eva.recordings.domain.models.AudioFileModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


@OptIn(UnstableApi::class)
internal class AudioVisualizerImpl(
    private val extractor: ExtractorsFactory,
    private val dataSource: DataSource.Factory,
    private val threadHandler: ThreadController,
) : AudioVisualizer {

    constructor(context: Context, threadHandler: ThreadController) : this(
        extractor = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true),
        dataSource = DefaultDataSource.Factory(context),
        threadHandler = threadHandler,
    )

    private var _decoder: MediaCodecPCMDataDecoder? = null
    private val _lock = Mutex()

    // raw amplitudes accumulated so far, grows only when the decoder
    // emits more points than the current capacity
    private var _amplitudes = FloatArray(INITIAL_AMPLITUDE_POINTS)
    private var _points = 0

    override val visualizerState: StateFlow<VisualizerState>
        field = MutableStateFlow(VisualizerState.NOT_STARTED)

    override val normalizedVisualization: StateFlow<FloatArray>
        field = MutableStateFlow(floatArrayOf())

    override suspend fun prepareVisualization(
        model: AudioFileModel,
        lifecycleOwner: LifecycleOwner,
        timePerPointInMs: Int,
    ): Result<Unit> = prepareVisualization(
        fileUri = model.fileUri,
        lifecycleOwner = lifecycleOwner,
        timePerPointInMs = timePerPointInMs,
    )

    override suspend fun prepareVisualization(
        fileUri: String,
        lifecycleOwner: LifecycleOwner,
        timePerPointInMs: Int,
    ): Result<Unit> = _lock.withLock {
        if (_decoder != null) {
            Log.d(TAG, "CLEAN DECODER TO PREPARE IT AGAIN")
            return Result.failure(DecoderExistsException())
        }

        // using a synchronized lock to clear the values
        synchronized(this) {
            _points = 0
            normalizedVisualization.value = floatArrayOf()
        }
        visualizerState.update { VisualizerState.NOT_STARTED }

        val handler = threadHandler.bindToLifecycle(lifecycleOwner)

        return try {
            val decoder = MediaCodecPCMDataDecoder(
                handler = handler,
                seekDurationMillis = timePerPointInMs,
            ).also { _decoder = it }

            Log.i(TAG, "MEDIA CODEC DECODER CREATED")

            // setup callbacks
            decoder.setOnBufferDecode(::updateVisuals)
            decoder.setOnComplete {
                Log.d(TAG, "DECODER JOB IS DONE RELEASING THE HANDLER")
                // release the objects
                releaseDecoder()
                // decoder work is done so we can kill it now
                if (handler != null) threadHandler.stopThread(handler)
            }
            decoder.initiateExtraction(extractor, dataSource, fileUri.toUri())
        } catch (e: Exception) {
            Log.e(TAG, "CANNOT DECODE THIS URI", e)
            Result.failure(e)
        }
    }

    @Synchronized
    private fun updateVisuals(array: FloatArray) {
        visualizerState.update { VisualizerState.RUNNING }

        // if the new size is larger update the float array size
        if (_points + array.size > _amplitudes.size) {
            val newSize = maxOf(_points + array.size, _amplitudes.size * 2)
            _amplitudes = _amplitudes.copyOf(newSize)
        }
        // else update the array
        array.copyInto(_amplitudes, _points)
        _points += array.size

        // only normalize and smooth based on the batch
        val snapshot = _amplitudes.copyOf(_points)

        // extension functions don't create a new array but update the current one
        snapshot.normalize().smoothen(.4f)
        normalizedVisualization.value = snapshot
    }

    private fun releaseDecoder() {
        try {
            _decoder?.cleanUp()
        } finally {
            Log.d(TAG, "DECODER CLEANED!")
            _decoder = null
            visualizerState.update { VisualizerState.FINISHED }
        }
    }

    override fun cleanUp() {
        Log.d(TAG, "CLEARING UP THE VISUALIZER")
        // release the objects
        releaseDecoder()
        // reset values
        synchronized(this) {
            _points = 0
            normalizedVisualization.value = floatArrayOf()
        }
    }

    companion object {
        private const val TAG = "PLAIN_VISUALIZER"
        private const val INITIAL_AMPLITUDE_POINTS = 2048
    }
}
