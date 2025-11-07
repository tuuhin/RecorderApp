package com.com.visualizer.data

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import com.com.visualizer.domain.AudioVisualizer
import com.com.visualizer.domain.ThreadController
import com.com.visualizer.domain.VisualizerState
import com.com.visualizer.domain.exception.DecoderExistsException
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

	private var _decoder: MediaCodecPCMDataDecoder? = null
	private val _lock = Mutex()

	private val _isReady = MutableStateFlow(VisualizerState.NOT_STARTED)
	private val _visualization = MutableStateFlow(floatArrayOf())

	override val visualizerState: StateFlow<VisualizerState>
		get() = _isReady

	override val normalizedVisualization: Flow<FloatArray>
		get() = _visualization.map { array -> array.normalize().smoothen(.4f) }
			.flowOn(Dispatchers.Default)
			.catch { err -> Log.d(TAG, "SOME ERROR", err) }


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
				_lock.withLock {
					_decoder = MediaCodecPCMDataDecoder(
						handler = handler,
						seekDurationMillis = timePerPointInMs,
					)
				}
				_decoder?.setOnBufferDecode(::updateVisuals)
				_decoder?.setOnComplete(::releaseObjects)
				_decoder?.initiateExtraction(context, fileUri.toUri())

				Result.success(Unit)
			} catch (e: Exception) {
				Log.e(TAG, "CANNOT DECODE THIS URI", e)
				Result.failure(e)
			}
		}
	}

	private fun updateVisuals(array: FloatArray) {
		_isReady.update { VisualizerState.RUNNING }
		_visualization.update { it + array }
	}

	private fun releaseObjects() {
		if (_lock.tryLock()) {
			try {
				Log.d(TAG, "CLEARING UP OBJECTS")
				_decoder?.cleanUp()
			} finally {
				_decoder = null
				_lock.unlock()
			}
		}
		_isReady.update { VisualizerState.FINISHED }
	}

	override fun cleanUp() {
		// reset values
		_visualization.update { floatArrayOf() }
		// release the objects
		releaseObjects()
	}

}