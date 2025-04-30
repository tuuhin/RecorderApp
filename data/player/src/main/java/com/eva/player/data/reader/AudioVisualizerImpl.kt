package com.eva.player.data.reader

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.util.Log
import androidx.core.net.toUri
import com.eva.player.domain.AudioVisualizer
import com.eva.player.domain.exceptions.InvalidMimeTypeException
import com.eva.recordings.domain.models.AudioFileModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

private const val TAG = "PLAIN_VISUALIZER"

class AudioVisualizerImpl(private val context: Context) : AudioVisualizer,
	CoroutineScope by MainScope() {

	private var _mediaCodec: MediaCodec? = null
	private var _extractor: MediaExtractor? = null

	private val _isReady = MutableStateFlow(false)
	override val isVisualReady: StateFlow<Boolean>
		get() = _isReady

	private val _visualization = MutableStateFlow<FloatArray>(floatArrayOf())
	override val visualization: Flow<FloatArray>
		get() = _visualization.map { array -> array.normalize() }
			.flowOn(Dispatchers.Default)

	override fun compressedVisualisation(length: Int): Flow<FloatArray> {
		return visualization.map { array -> array.compressFloatArray(length) }
			.flowOn(Dispatchers.Default)
	}

	override suspend fun prepareVisualization(model: AudioFileModel, timePerPointInMs: Int)
			: Result<Unit> {
		val result = async(Dispatchers.IO) {
			try {
				_extractor = MediaExtractor().apply {
					setDataSource(context, model.fileUri.toUri(), null)
				}
				val format = _extractor?.getTrackFormat(0)
				val mimetype = format?.mimeType
				if (mimetype?.startsWith("audio") != true) {
					Log.e(TAG, "WRONG MIME TYPE")
					return@async Result.failure(InvalidMimeTypeException())
				}

				_extractor?.selectTrack(0)

				val callback = MediaCodecCallback(
					extractor = _extractor,
					totalTime = format.duration,
					seekDuration = timePerPointInMs,
					scope = this@AudioVisualizerImpl,
					onBufferDecoded = { array ->
						_isReady.update { true }
						_visualization.update { array }
					},
				)

				Log.d(TAG, "MEDIA CODEC SET FOR MIME TYPE:$mimetype")
				Log.d(TAG, "CALLER THREAD : ${Thread.currentThread().name}")
				_mediaCodec?.reset()
				_mediaCodec = MediaCodec.createDecoderByType(mimetype).apply {
					configure(format, null, null, 0)
					setCallback(callback)
				}
				_mediaCodec?.start()

				Result.success(Unit)
			} catch (e: CancellationException) {
				Log.i(TAG, "COROUTINE WAS CANCELLED CANNOT MAKE VISUALIZER")
				throw e
			} catch (e: Exception) {
				Log.e(TAG, "Error decoding or processing audio", e)
				Result.failure(e)
			}
		}
		return result.await()
	}


	override fun cleanUp() {
		Log.d(TAG, "CLEAN UP FOR PLAIN VISUALIZER")
		cancel()
		_isReady.update { false }
		_extractor?.release()
		_mediaCodec?.reset()
		_mediaCodec?.release()
	}
}