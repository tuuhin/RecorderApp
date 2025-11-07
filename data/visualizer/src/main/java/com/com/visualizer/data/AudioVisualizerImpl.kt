package com.com.visualizer.data

import android.content.Context
import android.media.MediaExtractor
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import com.com.visualizer.domain.AudioVisualizer
import com.com.visualizer.domain.ThreadController
import com.com.visualizer.domain.exception.DecoderExistsException
import com.com.visualizer.domain.exception.ExtractorNoTrackFoundException
import com.com.visualizer.domain.exception.InvalidMimeTypeException
import com.eva.recordings.domain.models.AudioFileModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val TAG = "PLAIN_VISUALIZER"

internal class AudioVisualizerImpl(
	private val context: Context,
	private val threadHandler: ThreadController
) : AudioVisualizer {

	@Volatile
	private var _extractor: MediaExtractor? = null

	private val _isReady = MutableStateFlow(false)
	override val isVisualReady: StateFlow<Boolean>
		get() = _isReady

	private val _visualization = MutableStateFlow(floatArrayOf())
	override val normalizedVisualization: Flow<FloatArray>
		get() = _visualization.map { array -> array.normalize().smoothen(.4f) }
			.flowOn(Dispatchers.Default)
			.catch { err -> Log.d(TAG, "SOME ERROR", err) }

	private var _decoder: MediaCodecPCMDataDecoder? = null
	private val _lock = Mutex()

	override suspend fun prepareVisualization(
		model: AudioFileModel,
		lifecycleOwner: LifecycleOwner,
		timePerPointInMs: Int
	): Result<Unit> = prepareVisualization(
		fileUri = model.fileUri,
		lifecycleOwner = lifecycleOwner,
		timePerPointInMs
	)

	override suspend fun prepareVisualization(
		fileUri: String,
		lifecycleOwner: LifecycleOwner,
		timePerPointInMs: Int
	): Result<Unit> {

		if (_decoder != null) {
			Log.d(TAG, "CLEAN DECODER TO PREPARE IT AGAIN")
			return Result.failure(DecoderExistsException())
		}

		val handler = threadHandler.bindToLifecycle(lifecycleOwner)

		return withContext(Dispatchers.IO) {
			try {
				// clear an extractor if present
				_extractor?.release()
				_extractor = MediaExtractor().apply {
					setDataSource(context, fileUri.toUri(), null)
				}
				val format = _extractor?.getTrackFormat(0)
				val mimeType = format?.mimeType

				if (mimeType == null || !mimeType.startsWith("audio"))
					return@withContext Result.failure(InvalidMimeTypeException())

				if (_extractor?.trackCount == 0)
					return@withContext Result.failure(ExtractorNoTrackFoundException())

				_extractor?.selectTrack(0)

				_lock.withLock {
					_decoder = MediaCodecPCMDataDecoder(
						handler = handler,
						extractor = _extractor!!,
						totalTime = format.duration,
						seekDurationMillis = timePerPointInMs,
					)
				}
				_decoder?.setOnBufferDecode(::updateVisuals)
				_decoder?.setOnComplete(::releaseObjects)
				_decoder?.initiateCodec(format, mimeType)

				Result.success(Unit)
			} catch (e: Exception) {
				Log.e(TAG, "CANNOT DECODE THIS URI", e)
				Result.failure(e)
			}
		}
	}

	private fun updateVisuals(array: FloatArray) {
		_isReady.update { true }
		_visualization.update { it + array }
	}

	private fun releaseObjects() {
		if (_lock.tryLock()) {
			try {
				Log.d(TAG, "CLEARING UP OBJECTS")
				_decoder?.cleanUp()
				_extractor?.release()
			} finally {
				_extractor = null
				_decoder = null
				_lock.unlock()
			}
		} else {
			Log.d(TAG, "CANNOT ACQUIRE LOCK")
		}
	}

	override fun cleanUp() {

		// reset values
		_isReady.update { false }
		_visualization.update { floatArrayOf() }

		// cleanup
		releaseObjects()
	}

}