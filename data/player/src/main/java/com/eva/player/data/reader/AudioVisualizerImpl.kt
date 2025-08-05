package com.eva.player.data.reader

import android.content.ContentUris
import android.content.Context
import android.media.MediaExtractor
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import com.eva.player.domain.AudioVisualizer
import com.eva.player.domain.exceptions.DecoderExistsException
import com.eva.player.domain.exceptions.InvalidMimeTypeException
import com.eva.recordings.domain.models.AudioFileModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

private const val TAG = "PLAIN_VISUALIZER"

class AudioVisualizerImpl(private val context: Context) : AudioVisualizer {

	private val _scope = CoroutineScope(Dispatchers.Default)

	private var _extractor: MediaExtractor? = null
	private var _decoder: MediaCodecPCMDataDecoder? = null

	private val _isReady = MutableStateFlow(false)
	override val isVisualReady: StateFlow<Boolean>
		get() = _isReady

	private val _visualization = MutableStateFlow(floatArrayOf())
	override val normalizedVisualization: Flow<FloatArray>
		get() = _visualization.map { array -> array.normalize().smoothen(.4f) }
			.flowOn(Dispatchers.Default)

	override suspend fun prepareVisualization(model: AudioFileModel, timePerPointInMs: Int)
			: Result<Unit> = prepareVisualization(fileUri = model.fileUri, timePerPointInMs)

	override suspend fun prepareVisualization(fileId: Long, timePerPointInMs: Int): Result<Unit> {
		val uri = ContentUris.withAppendedId(
			MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
			fileId
		)
		return prepareVisualization(fileUri = uri.toString(), timePerPointInMs)
	}

	override suspend fun prepareVisualization(fileUri: String, timePerPointInMs: Int)
			: Result<Unit> {
		return withContext(Dispatchers.IO) {
			if (_decoder != null) {
				Log.d(TAG, "CLEAN DECODER TO PREPARE IT AGAIN")
				return@withContext Result.failure(DecoderExistsException())
			}

			try {
				_extractor = MediaExtractor().apply {
					setDataSource(context, fileUri.toUri(), null)
				}
				val format = _extractor?.getTrackFormat(0)
				val mimetype = format?.mimeType
				if (mimetype?.startsWith("audio") != true) {
					Log.e(TAG, "WRONG MIME TYPE")
					return@withContext Result.failure(InvalidMimeTypeException())
				}
				_extractor?.selectTrack(0)

				_decoder = MediaCodecPCMDataDecoder(
					extractor = _extractor,
					scope = _scope,
					totalTime = format.duration,
					seekDuration = timePerPointInMs,
					onBufferDecoded = { array ->
						_isReady.update { true }
						_visualization.update { it + array }
					},
				)
				Log.d(TAG, "MEDIA CODEC SET FOR MIME TYPE:$mimetype")
				_decoder?.initiateCodec(format, mimetype)
				Result.success(Unit)
			} catch (e: Exception) {
				Log.e(TAG, "Error decoding or processing audio", e)
				Result.failure(e)
			}
		}
	}

	override fun cleanUp() {
		_scope.cancel()
		_isReady.update { false }

		Log.d(TAG, "MEDIA CODEC IS RELEASED")
		_decoder?.cleanUp()
		_decoder = null

		Log.d(TAG, "MEDIA EXTRACTOR IS RELEASED")
		_extractor?.release()
		_extractor = null
	}
}