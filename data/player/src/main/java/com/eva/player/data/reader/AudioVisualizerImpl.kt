package com.eva.player.data.reader

import android.content.Context
import android.media.MediaExtractor
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import com.eva.player.domain.AudioVisualizer
import com.eva.player.domain.exceptions.DecoderExistsException
import com.eva.player.domain.exceptions.ExtractorNoTrackFoundException
import com.eva.player.domain.exceptions.InvalidMimeTypeException
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

internal class AudioVisualizerImpl(private val context: Context) : AudioVisualizer {

	@Volatile
	private var _extractor: MediaExtractor? = null

	private val _threadHandler by lazy { ThreadLifecycleHandler("ComputeThread") }

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
	): Result<Unit> = _lock.withLock {

		if (_decoder != null) {
			Log.d(TAG, "CLEAN DECODER TO PREPARE IT AGAIN")
			return Result.failure(DecoderExistsException())
		}

		withContext(Dispatchers.IO) {
			try {
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

				_decoder = MediaCodecPCMDataDecoder(
					extractor = _extractor!!,
					totalTime = format.duration,
					seekDurationMillis = timePerPointInMs,
				).apply {
					setOnBufferDecode { array ->
						_isReady.update { true }
						_visualization.update { it + array }
					}
				}

				val handler = _threadHandler.bindToLifecycle(lifecycleOwner)
				_decoder?.setOnComplete(::releaseObjects)
				_decoder?.initiateCodec(format, mimeType, handler)

				Result.success(Unit)
			} catch (e: Exception) {
				Log.e(TAG, "CANNOT DECODE THIS URI", e)
				Result.failure(e)
			}
		}
	}

	private fun releaseObjects() {
		Log.d(TAG, "CLEARING UP OBJECTS")
		_decoder?.cleanUp()
		_decoder = null

		_extractor?.release()
		_extractor = null
	}

	override fun cleanUp() {
		// reset values
		_isReady.update { false }
		_visualization.update { floatArrayOf() }

		// cleanup
		releaseObjects()
	}

}