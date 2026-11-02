package com.com.visualizer.data

import android.content.ContentResolver
import android.content.ContentUris
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
import com.com.visualizer.data.cache.FileHashGenerator
import com.com.visualizer.data.cache.VisualCacheImpl
import com.com.visualizer.domain.AudioVisualizer
import com.com.visualizer.domain.ThreadController
import com.com.visualizer.domain.VisualizerCache
import com.com.visualizer.domain.VisualizerState
import com.com.visualizer.domain.exception.DecoderExistsException
import com.com.visualizer.domain.model.VisualizerCacheEntry
import com.eva.recordings.domain.models.AudioFileModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(UnstableApi::class)
internal class AudioVisualizerImpl(
    private val context: Context,
    private val cache: VisualizerCache,
    private val extractor: ExtractorsFactory,
    private val dataSource: DataSource.Factory,
    private val threadHandler: ThreadController,
) : AudioVisualizer {

    internal constructor(context: Context, threadHandler: ThreadController) : this(
        context = context,
        cache = VisualCacheImpl(context.filesDir),
        extractor = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true),
        dataSource = DefaultDataSource.Factory(context),
        threadHandler = threadHandler,
    )

    private val hashGenerator by lazy { FileHashGenerator(context) }

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

        // will support either a file uri or a content uri
        val uri = fileUri.toUri()
        val cacheKey = if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            try {
                ContentUris.parseId(uri).toString()
            } catch (_: Exception) {
                fileUri
            }
        } else fileUri

        // cleanup and reset visualizer state
        synchronized(this) {
            _points = 0
            normalizedVisualization.value = floatArrayOf()
        }
        visualizerState.update { VisualizerState.NOT_STARTED }

        // a hash string and then we check our hash results
        val hash = hashGenerator.generateHash(fileUri.toUri())

        if (hash != null) {
            // read in the cache entry if good update the visuals
            val entry = cache.getCacheEntry(cacheKey)
            if (entry != null && entry.hash == hash && entry.timePerPointMs == timePerPointInMs) {
                Log.i(TAG, "CACHE HIT FOR AUDIO $cacheKey, SKIPPING THE DECODER")
                applyVisualsFromCache(entry)
                return Result.success(Unit)
            }
        }

        // if not found in cache fallback to reading it via new MediaCodec
        if (_decoder != null) {
            Log.d(TAG, "CLEAN DECODER TO PREPARE IT AGAIN")
            return Result.failure(DecoderExistsException())
        }

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
                // to decode ran to completion so the points are worth caching
                if (hash != null) persistCacheEntry(cacheKey, hash, timePerPointInMs)
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
            Log.i(TAG, "CLEARING DECODER")
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

    private fun applyVisualsFromCache(entry: VisualizerCacheEntry) {
        val snapshot = synchronized(this) {
            // update the data from entry
            _amplitudes = entry.entryFloats
            _points = entry.entryFloats.size
            _amplitudes.copyOf(_points)
        }

        // update the snapshot and mark it as completed
        snapshot.normalize().smoothen(.4f)
        normalizedVisualization.value = snapshot
        visualizerState.update { VisualizerState.FINISHED }
    }


    private suspend fun persistCacheEntry(key: String, hash: String, timePerPointMs: Int) {
        val points = synchronized(this) {
            if (_points <= 0) return
            _amplitudes.copyOf(_points)
        }

        Log.d(TAG, "SAVING ${points.size} POINTS TO THE VISUALIZER CACHE")
        val entry = VisualizerCacheEntry(hash, points, timePerPointMs)
        cache.setCacheEntry(key, entry)
    }

    companion object {
        private const val TAG = "PLAIN_VISUALIZER"
        private const val INITIAL_AMPLITUDE_POINTS = 2048
    }
}
