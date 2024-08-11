package com.eva.recorderapp.voice_recorder.data.player

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.player.PlayerFileProvider
import com.eva.recorderapp.voice_recorder.domain.player.exceptions.InvalidMimeTypeException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.nio.ByteBuffer
import kotlin.math.pow
import kotlin.math.sqrt

private const val TAG = "AMPLITUDE_READER"

class AudioAmplitudeReader(
	private val context: Context,
	private val fileProvider: PlayerFileProvider,
) {

	private var extractor: MediaExtractor? = null
	private var mediaCodec: MediaCodec? = null

	private var _channels = 1
	private var _pcmEncodingBit = 16
	private val perSamplePoints = 1_000
	private var sampleSum = 0.0f

	private val _isEofReached = MutableStateFlow(false)
	private var sampleCount = MutableStateFlow(0)
	private val _sampleData = MutableStateFlow<List<Float>>(emptyList())

	val samples = _sampleData.filter { it.isNotEmpty() }
		.map { data -> data.paddedList().normalize() }

	val isLoadingCompleted = _isEofReached.asSharedFlow()

	suspend fun evaluteSamplesGraphFromAudioId(audioId: Long): Resource<Unit, Exception> {
		return coroutineScope {
			try {
				val audioUri = fileProvider.providesAudioFileUri(audioId)
				Log.d(TAG, "EVALUTATING FOR URI: $audioUri")
				startMediaDecoder(audioUri)
				Resource.Success(Unit)
			} catch (e: Exception) {
				e.printStackTrace()
				Resource.Error(e)
			}
		}
	}

	private val codecCallback = object : MediaCodec.Callback() {

		override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
			if (_isEofReached.value) {
				Log.d(TAG, "END OF BUFFER REACHED...")
				return
			}
			extractor?.let { extrac ->
				if (index >= 0) {
					// get the input buffer
					val inputBuffer = codec.getInputBuffer(index) ?: return
					// read the sample data and store it in the buffer
					val sampleSize = extrac.readSampleData(inputBuffer, 0)
					if (sampleSize < 0) {
						//no data so end of stream
						codec.signalEndOfInputStream()
						Log.d(TAG, "END OF INPUT STREAM")
						_isEofReached.update { true }
					} else {
						// enque the buffer with the buffer info
						codec.queueInputBuffer(
							index, 0, sampleSize, extrac.sampleTime, 0
						)
						//advance the extractor to read the next sample
						val advance = extrac.advance()
						if (!advance) {
							Log.d(TAG, "CANNOT ADVANCE EXTRACTOR ANY MORE")
							_isEofReached.update { true }
						}
					}
				}
			}
		}

		override fun onOutputBufferAvailable(
			codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo
		) {
			// stop if end of buffer is reached
			if (info.isEof) {
				Log.d(TAG, "END OF BUFFER REACHED...")
				codec.stop()
				return
			}
			if (info.size > 0) {// byte buffer for the ouput buffer
				val outputBuffer = codec.getOutputBuffer(index)
				outputBuffer?.position(info.offset)
				// each will prepare the output
				when (_pcmEncodingBit) {
					8 -> outputBuffer?.handle8bit(info.size)
					16 -> outputBuffer?.handle16bit(info.size)
					32 -> outputBuffer?.handle32bit(info.size)
				}
				// release the output buffer
				codec.releaseOutputBuffer(index, false)
			}
		}

		override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
			Log.e(TAG, "ERROR HAPPENED", e)
		}

		override fun onCryptoError(codec: MediaCodec, e: MediaCodec.CryptoException) = Unit

		override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
			Log.d(TAG, "OUTPUT FORMAT CHANGED")
			_channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
			_pcmEncodingBit = if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
				when (format.getInteger(MediaFormat.KEY_PCM_ENCODING)) {
					AudioFormat.ENCODING_PCM_8BIT -> 8
					AudioFormat.ENCODING_PCM_16BIT -> 16
					AudioFormat.ENCODING_PCM_FLOAT -> 32
					else -> 16
				}
			} else 16
		}
	}


	private suspend fun startMediaDecoder(uri: Uri) {
		extractor = MediaExtractor().apply {
			setDataSource(context, uri, null)
		}
		val format = extractor?.getTrackFormat(0) ?: return

		val mimeType = format.getString(MediaFormat.KEY_MIME)
		Log.d(TAG, "MIME TYPE SELECTED $mimeType")
		if (mimeType == null) {
			Log.d(TAG, "AMPLITUDE READER")
			throw InvalidMimeTypeException()
		}
		// track seleted
		extractor?.selectTrack(0)
		Log.d(TAG, "TRACK SELECTED")
		// creates the decorder
		try {
			// configure the decoder
			mediaCodec = MediaCodec.createDecoderByType(mimeType).apply {
				configure(format, null, null, 0)
				setCallback(codecCallback)
			}
			//start the decorder
			mediaCodec?.start()
			Log.d(TAG, "MEDIA CODEC STARTED SUCCESSFULLY")
		} catch (e: IllegalStateException) {
			Log.e(TAG, "ILLEGAL STATE FOUND PLEASE CHECK", e)
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	private fun rms(value: Float) {
		if (sampleCount.value == perSamplePoints) {
			val rms = sqrt(sampleSum / perSamplePoints)
			_sampleData.update { it + rms.toFloat() }
			sampleCount.update { 0 }
			sampleSum = 0.0f
		}
		sampleCount.update { it + 1 }
		sampleSum += value.toDouble().pow(2.0).toFloat()
	}


	private fun ByteBuffer.handle8bit(bufferInfoSize: Int) {
		val times = bufferInfoSize / if (_channels == 2) 2 else 1
		val divisor = 2f.pow(7f)
		repeat(times) {
			val result = get().toInt() / divisor
			if (_channels == 2) {
				// skip the next value
				get()
			}
			rms(result)
		}
	}

	private fun ByteBuffer.handle16bit(bufferInfoSize: Int) {
		val times = bufferInfoSize / if (_channels == 2) 8 else 4
		val divisor = 2f.pow(15)
		repeat(times) {
			val first = get().toInt()
			val second = get().toInt() shl 8
			val value = (first or second) / divisor
			if (_channels == 2) {
				//skipping the next 3 values
				repeat(2) { get() }
			}
			rms(value)
		}
	}

	private fun ByteBuffer.handle32bit(bufferInfoSize: Int) {
		val times = bufferInfoSize / if (_channels == 2) 8 else 4
		val divisor = 2f.pow(31)

		repeat(times) {
			val first = get().toLong()
			val second = get().toLong() shl 8
			val third = get().toLong() shl 16
			val forth = get().toLong() shl 24
			val value = (first or second or third or forth) / divisor
			if (_channels == 2) {
				//skipping the next 4 values
				repeat(4) { get() }
			}
			rms(value)
		}
	}

	fun List<Float>.normalize(): FloatArray {
		val max = maxOrNull() ?: 0f
		val min = minOrNull() ?: 0f
		val range = max - min
		if (range <= 0) return floatArrayOf()

		return map { amt ->
			((amt - min) / range).coerceIn(0f..1f)
		}.toFloatArray()
	}

	fun List<Float>.paddedList(minSize: Int = 100, builder: (Int) -> Float = { 0f }): List<Float> {
		val extraSize = minSize - size
		return if (extraSize < 0) this
		else this + List(extraSize, builder)
	}

	//clears the extractor
	fun clearResources() {
		Log.i(TAG, "CLEARING MEDIA EXTRACTOR AND MEDIA CODEC")
		extractor?.release()
		mediaCodec?.release()
		extractor = null
		mediaCodec = null
	}
}

private val MediaCodec.BufferInfo.isEof: Boolean
	get() = flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
