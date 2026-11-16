@file:kotlin.OptIn(ExperimentalAtomicApi::class)

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
import com.com.visualizer.data.ext.normalize
import com.com.visualizer.data.ext.resampleRms
import com.com.visualizer.data.ext.smoothen
import com.com.visualizer.data.models.ActiveRequest
import com.com.visualizer.data.models.VisualizationSnapshot
import com.com.visualizer.domain.AudioVisualizer
import com.com.visualizer.domain.ThreadController
import com.com.visualizer.domain.VisualizationData
import com.com.visualizer.domain.VisualizerCache
import com.com.visualizer.domain.VisualizerSession
import com.com.visualizer.domain.VisualizerState
import com.com.visualizer.domain.exception.VisualizationOwnershipException
import com.com.visualizer.domain.model.VisualizerCacheEntry
import com.eva.recordings.domain.models.AudioFileModel
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.concurrent.withLock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
internal class AudioVisualizerImpl(
    private val context: Context,
    private val cache: VisualizerCache,
    private val extractor: ExtractorsFactory,
    private val dataSource: DataSource.Factory,
    private val threadHandler: ThreadController,
) : AudioVisualizer {

    private val nextGeneration = AtomicLong(0)

    internal constructor(context: Context, threadHandler: ThreadController) : this(
        context = context,
        cache = VisualCacheImpl(context.filesDir),
        extractor = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true),
        dataSource = DefaultDataSource.Factory(context),
        threadHandler = threadHandler,
    )

    private val hashGenerator by lazy { FileHashGenerator(context) }
    private val _lock = Mutex()
    private val _reqLock = ReentrantLock()
    private val _scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val snapshot = MutableStateFlow(VisualizationSnapshot())
    private var activeRequest: ActiveRequest? = null
    private var amplitudes = FloatArray(INITIAL_AMPLITUDE_POINTS)
    private var points = 0

    override fun observeVisuals(owner: VisualizerSession): Flow<VisualizationData> = snapshot
        .map { current ->
            if (current.owner == owner) VisualizationData(current.state, current.values)
            else EMPTY_DATA
        }
        .distinctUntilChanged()

    override suspend fun prepareVisualization(
        model: AudioFileModel,
        lifecycleOwner: LifecycleOwner,
        session: VisualizerSession,
        timePerPointInMs: Int,
    ): Result<Unit> = prepareVisualization(
        fileUri = model.fileUri,
        lifecycleOwner = lifecycleOwner,
        session = session,
        timePerPointInMs = timePerPointInMs,
    )

    override suspend fun prepareVisualization(
        fileUri: String,
        lifecycleOwner: LifecycleOwner,
        session: VisualizerSession,
        timePerPointInMs: Int,
    ): Result<Unit> = _lock.withLock {
        // check if time per points is set
        if (timePerPointInMs <= 0) return@withLock Result.failure(IllegalArgumentException("Point duration must be positive"))

        // read in the cacheKey
        val cacheKey = buildCacheKey(fileUri)

        // check if we have and have an active request if yes we re-hash the file
        val previousRequest = _reqLock.withLock { activeRequest }
        val sameIdentity = previousRequest?.let { isSameRequestIdentity(it, fileUri) } == true
        val existingHash = if (sameIdentity) hashGenerator.generateHash(fileUri) else null

        // now if we have a previous request resue the request
        if (previousRequest != null && canReuseRequest(previousRequest, fileUri, existingHash)) {
            if (reuseRequest(previousRequest, lifecycleOwner, session, timePerPointInMs)) {
                return@withLock Result.success(Unit)
            }
        }
        // if we cannot use the previous request release thre request
        if (previousRequest != null) releaseRequest(previousRequest)

        // create a new request
        val request = _reqLock.withLock {
            ActiveRequest(
                generation = nextGeneration.incrementAndFetch(),
                playerId = session.audioId,
                fileUri = fileUri,
                session = session,
                cacheKey = cacheKey,
                timePerPointInMs = timePerPointInMs,
                requestedPointMs = timePerPointInMs,
            ).also {
                activeRequest = it
                amplitudes = FloatArray(INITIAL_AMPLITUDE_POINTS)
                points = 0
                publishSnapshot(VisualizationSnapshot(owner = session))
            }
        }

        try {
            request.hash = if (sameIdentity) existingHash else hashGenerator.generateHash(fileUri)
            if (!isCurrentOwner(request))
                return@withLock Result.failure(VisualizationOwnershipException())

            if (loadCachedVisuals(request)) return@withLock Result.success(Unit)

            startDecoding(request, lifecycleOwner, fileUri)

            if (!isCurrentOwner(request)) Result.failure(VisualizationOwnershipException())
            else Result.success(Unit)

        } catch (e: CancellationException) {
            withContext(NonCancellable) {
                if (isCurrentRequest(request)) releaseRequest(request)
            }
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "CANNOT DECODE THIS URI", e)
            withContext(NonCancellable) {
                if (isCurrentRequest(request)) {
                    releaseResources(request)
                    _reqLock.withLock {
                        if (activeRequest === request && request.session != null) {
                            publishSnapshot(snapshot.value.copy(state = VisualizerState.FAILED))
                        }
                    }
                }
            }
            Result.failure(e)
        }
    }

    private fun isSameRequestIdentity(request: ActiveRequest, fileUri: String): Boolean =
        request.playerId == snapshot.value.owner?.audioId &&
            request.fileUri == fileUri &&
            request.session != null &&
            snapshot.value.owner == request.session &&
            snapshot.value.state != VisualizerState.FAILED

    private fun canReuseRequest(request: ActiveRequest, fileUri: String, existingHash: String?): Boolean =
        isSameRequestIdentity(request, fileUri) &&
            ((existingHash != null && existingHash == request.hash) ||
                (existingHash == null && request.hash == null && request.decoder != null))

    private fun reuseRequest(
        request: ActiveRequest,
        lifecycleOwner: LifecycleOwner,
        session: VisualizerSession,
        timePerPointInMs: Int,
    ): Boolean {
        val existingHandler = request.handler
        val handlerIsCurrent = existingHandler == null || runCatching {
            threadHandler.bindToLifecycle(lifecycleOwner) === existingHandler &&
                existingHandler.looper.thread.isAlive
        }.getOrDefault(false)
        if (!handlerIsCurrent) return false

        _reqLock.withLock {
            if (activeRequest === request && request.session != null) {
                request.session = session
                request.requestedPointMs = timePerPointInMs
                publishSnapshot(snapshot.value.copy(owner = session, values = valuesFor(request)))
            }
        }
        return snapshot.value.owner == session
    }

    private suspend fun loadCachedVisuals(request: ActiveRequest): Boolean {
        val hash = request.hash ?: return false
        val entry = cache.getCacheEntry(request.cacheKey)
        if (!isCurrentOwner(request)) return false
        if (entry == null || entry.hash != hash) return false

        Log.i(TAG, "CACHE HIT FOR AUDIO ${request.cacheKey}, SKIPPING THE DECODER")
        applyVisualsFromCache(request, entry)
        return true
    }

    private suspend fun startDecoding(request: ActiveRequest, lifecycleOwner: LifecycleOwner, uri: String) {
        val handler = checkNotNull(threadHandler.bindToLifecycle(lifecycleOwner)) {
            "Unable to start the visualizer thread"
        }
        request.handler = handler

        val decoder = MediaCodecPCMDataDecoder(handler = handler, seekDurationMillis = request.timePerPointInMs)
            .also { request.decoder = it }
        decoder.setOnBufferDecode { array -> updateVisuals(request, array) }
        decoder.setOnComplete { completeRequest(request) }
        decoder.setOnFailure { failure -> failRequest(request, failure) }

        val extractionResult = decoder.initiateExtraction(extractor, dataSource, uri.toUri())
        extractionResult.exceptionOrNull()?.let { throw it }
    }

    private fun publishSnapshot(value: VisualizationSnapshot) {
        snapshot.value = value
    }

    override fun cleanUp(owner: VisualizerSession) {
        val request = invalidateOwner(owner) ?: return
        runBlocking { releaseOwnedRequest(request) }
    }

    override suspend fun cleanUpAndAwait(owner: VisualizerSession) {
        invalidateOwner(owner)?.let { releaseOwnedRequest(it) }
    }

    private fun invalidateOwner(owner: VisualizerSession): ActiveRequest? = _reqLock.withLock {
        if (snapshot.value.owner != owner) return null
        activeRequest?.takeIf { it.session == owner }?.also { it.session = null }
            .also { publishSnapshot(VisualizationSnapshot()) }
    }

    private suspend fun releaseOwnedRequest(request: ActiveRequest) {
        _lock.withLock {
            val shouldRelease = _reqLock.withLock {
                activeRequest === request && request.session == null
            }
            if (shouldRelease) releaseRequest(request)
        }
    }

    private fun updateVisuals(request: ActiveRequest, array: FloatArray) {
        _reqLock.withLock {
            val owner = request.session ?: return
            if (activeRequest !== request) return

            if (points + array.size > amplitudes.size) {
                val newSize = maxOf(points + array.size, amplitudes.size * 2)
                amplitudes = amplitudes.copyOf(newSize)
            }
            array.copyInto(amplitudes, points)
            points += array.size

            val values = valuesFor(request)
            publishSnapshot(VisualizationSnapshot(owner, VisualizerState.RUNNING, values))
        }
    }

    private suspend fun completeRequest(request: ActiveRequest) {
        _lock.withLock {
            val owner = _reqLock.withLock {
                request.session?.takeIf { activeRequest === request }
            } ?: return

            val hasPoints = _reqLock.withLock { activeRequest === request && points > 0 }
            if (!hasPoints) {
                releaseResources(request)
                _reqLock.withLock {
                    if (activeRequest === request && request.session == owner) {
                        publishSnapshot(snapshot.value.copy(state = VisualizerState.FAILED))
                    }
                }
                return@withLock
            }

            try {
                val hash = request.hash
                if (hash != null && isCurrentOwner(request)) {
                    persistCacheEntry(request.cacheKey, hash, request.timePerPointInMs)
                }
            } catch (e: CancellationException) {
                withContext(NonCancellable) { releaseResources(request) }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "UNABLE TO CACHE VISUALIZATION", e)
            }
            releaseResources(request)
            _reqLock.withLock {
                if (activeRequest === request && request.session == owner) {
                    publishSnapshot(snapshot.value.copy(state = VisualizerState.FINISHED))
                }
            }
        }
    }

    private fun failRequest(request: ActiveRequest, failure: Throwable) {
        _reqLock.withLock {
            if (activeRequest !== request || request.session == null) return
            Log.e(TAG, "ASYNCHRONOUS VISUALIZATION FAILURE", failure)
            publishSnapshot(snapshot.value.copy(state = VisualizerState.FAILED))
        }

        _scope.launch {
            _lock.withLock {
                val shouldRelease = _reqLock.withLock {
                    activeRequest === request && request.session != null &&
                        snapshot.value.state == VisualizerState.FAILED
                }
                if (shouldRelease) releaseResources(request)
            }
        }
    }

    private suspend fun releaseRequest(request: ActiveRequest) {
        _reqLock.withLock {
            if (activeRequest === request) {
                request.session = null
                publishSnapshot(VisualizationSnapshot())
            }
        }
        releaseResources(request)
        _reqLock.withLock {
            if (activeRequest === request) activeRequest = null
        }
    }

    private suspend fun releaseResources(request: ActiveRequest) {
        val decoder = request.decoder.also { request.decoder = null }
        val handler = request.handler.also { request.handler = null }

        try {
            decoder?.cleanUp()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "FAILED TO RELEASE DECODER", e)
        } finally {
            if (handler != null) threadHandler.stopThread(handler)
        }
    }

    private fun applyVisualsFromCache(request: ActiveRequest, entry: VisualizerCacheEntry) {
        _reqLock.withLock {
            if (activeRequest !== request || request.session == null) return
            require(entry.timePerPointMs > 0) { "Cached point duration must be positive" }
            require(entry.entryFloats.isNotEmpty()) { "Cached waveform contains no points" }
            request.timePerPointInMs = entry.timePerPointMs
            amplitudes = entry.entryFloats.copyOf()
            points = entry.entryFloats.size
            val values = valuesFor(request)
            publishSnapshot(VisualizationSnapshot(request.session, VisualizerState.FINISHED, values))
        }
    }

    private fun valuesFor(request: ActiveRequest): FloatArray = amplitudes.copyOf(points)
        .resampleRms(request.timePerPointInMs, request.requestedPointMs)
        .normalize()
        .smoothen(.4f)

    private suspend fun persistCacheEntry(key: String, hash: String, timePerPointMs: Int) {
        val values = _reqLock.withLock {
            if (points <= 0) return
            amplitudes.copyOf(points)
        }
        Log.d(TAG, "SAVING ${values.size} POINTS TO THE VISUALIZER CACHE")
        cache.setCacheEntry(key, VisualizerCacheEntry(hash, values, timePerPointMs))
    }

    private fun isCurrentOwner(request: ActiveRequest): Boolean = _reqLock.withLock {
        activeRequest === request && request.session != null
    }

    private fun isCurrentRequest(request: ActiveRequest): Boolean = _reqLock.withLock {
        activeRequest === request
    }


    /**
     * If it's a content uri then use the content id otherwise use the whole
     * file uri as a cache key
     */
    private fun buildCacheKey(uri: String): String {
        val contentUri = uri.toUri()
        return if (contentUri.scheme == ContentResolver.SCHEME_CONTENT) {
            try {
                ContentUris.parseId(contentUri).toString()
            } catch (_: Exception) {
                uri
            }
        } else uri
    }

    companion object {
        private const val TAG = "PLAIN_VISUALIZER"
        private const val INITIAL_AMPLITUDE_POINTS = 2048
        private val EMPTY_VISUALS = floatArrayOf()
        private val EMPTY_DATA = VisualizationData(values = EMPTY_VISUALS)
    }
}
