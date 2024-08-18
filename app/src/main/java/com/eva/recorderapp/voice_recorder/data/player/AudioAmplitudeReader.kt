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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.pow
import kotlin.math.sqrt

private const val TAG = "AMPLITUDE_READER"
typealias RMSValues = List<Float>

private const val TWO_POWER_15 = 32_768f
private const val TWO_POWER_31 = 2_14_74_83_648f
private const val TWO_POWER_7 = 128f
private const val RMS_VALUES_MIN_SIZE = 120

class AudioAmplitudeReader(
	private val context: Context,
	private val fileProvider: PlayerFileProvider,
) {

	private var extractor: MediaExtractor? = null
	private var mediaCodec: MediaCodec? = null

	private var _channels = 1
	private var _pcmEncodingBit = 16
	private val perSamplePoints = 1_000
	private var squaredSum = 0.0f

	private var sampleCount = AtomicInteger(0)

	private val _isEofReached = MutableStateFlow(false)
	private val _sampleData = MutableStateFlow<RMSValues>(emptyList())

	/**
	 * Codec state provides which state the [MediaCodec] is in
	 * ensuring correctly calling methods
	 */
	private val _codecState = MutableStateFlow(MediaCodecState.STOP)

	val samples = _sampleData.filter { it.isNotEmpty() }
		.map { data -> data.paddedList().normalize() }

	val isLoadingCompleted = _isEofReached.asSharedFlow()

	suspend fun evaluteSamplesGraphFromAudioId(audioId: Long): Resource<Unit, Exception> {
		return coroutineScope {
			try {
				// clearing the values
				_sampleData.update { emptyList() }
				_isEofReached.update { false }
				// get the audio uri
				val audioUri = fileProvider.providesAudioFileUri(audioId)
				Log.d(TAG, "EVALUTATING FOR URI: $audioUri")
				// start media codec
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
			if (_codecState.value != MediaCodecState.EXEC) {
				Log.d(TAG, "STATE SHOULD BE EXECUTING FOUND ${_codecState.value}")
				return
			}
			try {
				val extrac = extractor ?: return
				if (index < 0) return
				// get the input buffer
				val inputBuffer = codec.getInputBuffer(index) ?: return
				// read the sample data and store it in the buffer
				val sampleSize = extrac.readSampleData(inputBuffer, 0)
				if (sampleSize < 0) {
					//no data so end of stream
					codec.signalEndOfInputStream()
					Log.d(TAG, "END OF INPUT STREAM")
					_codecState.update { MediaCodecState.END }
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
			} catch (e: IllegalStateException) {
				Log.e(TAG, "MEDIA CODEC IS NOT IN EXECUTION STATE")
			}
		}

		override fun onOutputBufferAvailable(
			codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo
		) {
			if (_codecState.value != MediaCodecState.EXEC) {
				Log.d(TAG, "STATE SHOULD BE EXECUTING FOUND ${_codecState.value}")
				return
			}
			try {
				// stop if end of buffer is reached
				if (info.isEof) {
					Log.d(TAG, "END OF BUFFER REACHED...")
					codec.stop()
					_codecState.update { MediaCodecState.STOP }
					return
				}
				if (info.size > 0) {
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
			} catch (e: IllegalStateException) {
				Log.e(TAG, "MEDIA CODEC IS NOT IN EXECUTION STATE")
			}
		}

		override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
			_codecState.update { MediaCodecState.STOP }
			Log.e(TAG, "ERROR HAPPENED", e)
		}

		override fun onCryptoError(codec: MediaCodec, e: MediaCodec.CryptoException) = Unit

		override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
			Log.d(TAG, "CHECKING METADATA FOR THE MEDIA")
			_channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
			_pcmEncodingBit = if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
				when (format.getInteger(MediaFormat.KEY_PCM_ENCODING)) {
					AudioFormat.ENCODING_PCM_8BIT -> 8
					AudioFormat.ENCODING_PCM_16BIT -> 16
					AudioFormat.ENCODING_PCM_32BIT -> 32
					else -> 16
				}
			} else 16
		}
	}


	private suspend fun startMediaDecoder(uri: Uri) = withContext(Dispatchers.Default) {
		extractor = MediaExtractor().apply {
			setDataSource(context, uri, null)
		}
		val format = extractor?.getTrackFormat(0) ?: return@withContext

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
			// preparing the media codec
			_codecState.update { MediaCodecState.STOP }
			// configure the decoder
			mediaCodec = MediaCodec.createDecoderByType(mimeType).apply {
				configure(format, null, null, 0)
				setCallback(codecCallback)
			}
			//start the decorder
			mediaCodec?.start()
			_codecState.update { MediaCodecState.EXEC }
			Log.d(TAG, "MEDIA CODEC STARTED SUCCESSFULLY")
		} catch (e: IllegalStateException) {
			Log.e(TAG, "ILLEGAL STATE FOUND PLEASE CHECK", e)
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	private fun evaluate_rms(value: Float) {
		val count = sampleCount.getAndIncrement()
		if (count == perSamplePoints) {
			// the root mean square value of the sample points
			val rms: Float = sqrt(squaredSum / perSamplePoints)
			_sampleData.update { it + rms }
			sampleCount.set(0)
			squaredSum = 0.0f
		}
		squaredSum += value.pow(2)
	}


	private fun ByteBuffer.handle8bit(bufferInfoSize: Int) {
		val times = bufferInfoSize / if (_channels == 2) 2 else 1
		repeat(times) {
			val result = get().toInt() / TWO_POWER_7
			if (_channels == 2) {
				// skip the next value
				get()
			}
			evaluate_rms(result)
		}
	}

	private fun ByteBuffer.handle16bit(bufferInfoSize: Int) {
		val times = bufferInfoSize / if (_channels == 2) 8 else 4
		repeat(times) {
			val first = get().toInt()
			val second = get().toInt() shl 8
			val value = (first or second) / TWO_POWER_15
			if (_channels == 2) {
				//skipping the next 3 values
				repeat(2) { get() }
			}
			evaluate_rms(value)
		}
	}

	private fun ByteBuffer.handle32bit(bufferInfoSize: Int) {
		val times = bufferInfoSize / if (_channels == 2) 8 else 4

		repeat(times) {
			val first = get().toLong()
			val second = get().toLong() shl 8
			val third = get().toLong() shl 16
			val forth = get().toLong() shl 24
			val value = (first or second or third or forth) / TWO_POWER_31
			if (_channels == 2) {
				//skipping the next 4 values
				repeat(4) { get() }
			}
			evaluate_rms(value)
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

	fun List<Float>.paddedList(
		minSize: Int = RMS_VALUES_MIN_SIZE,
		builder: (Int) -> Float = { 0f }
	): List<Float> {
		val extraSize = minSize - size
		return if (extraSize < 0) this
		else this + List(extraSize, builder)
	}

	//clears the extractor
	fun clearResources() {
		Log.i(TAG, "RELEASING MEDIA EXTRACTOR")
		extractor?.release()
		extractor = null
		Log.i(TAG, "RELEASING MEDIA CODEC")
		mediaCodec?.release()
		mediaCodec = null
		_codecState.update { MediaCodecState.END }
	}
}

private enum class MediaCodecState {
	/**
	 * [MediaCodec] state stopped
	 */
	STOP,

	/**
	 * [MediaCodec] state executing
	 */
	EXEC,

	/**
	 * [MediaCodec] state released
	 */
	END
}

private val MediaCodec.BufferInfo.isEof: Boolean
	get() = flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
