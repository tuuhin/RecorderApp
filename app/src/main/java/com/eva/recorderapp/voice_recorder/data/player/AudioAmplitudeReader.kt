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
import com.eva.recorderapp.voice_recorder.domain.player.RMSValues
import com.eva.recorderapp.voice_recorder.domain.player.WaveformsReader
import com.eva.recorderapp.voice_recorder.domain.player.exceptions.InvalidMimeTypeException
import com.eva.recorderapp.voice_recorder.domain.recorder.VoiceRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.DurationUnit

private const val TAG = "AMPLITUDE_READER"

private const val TWO_POWER_15 = 32_768f
private const val TWO_POWER_31 = 2_147_483_648L
private const val TWO_POWER_7 = 128f

class AudioAmplitudeReader(
	private val context: Context,
	private val fileProvider: PlayerFileProvider,
) : WaveformsReader {

	private var extractor: MediaExtractor? = null
	private var mediaCodec: MediaCodec? = null

	// no of channels
	private var _channels = 1

	// pcm encoding
	private var _pcmEncodingBit = 16

	// rms sum
	private var _squaredSum = 0.0f

	// selected audio duration
	private var _audioDuration = 0.microseconds

	// how many samples are read
	private var _sampleCount = AtomicInteger(0)

	// how many sample points will be taken as 1
	private var _perSamplePoints = 0

	// total no. of expected point
	private var _expectedPoints = 0

	/**
	 * Codec state provides which state the [MediaCodec] is in
	 * ensuring correctly calling methods
	 */
	private val _codecState = MutableStateFlow(MediaCodecState.STOP)
	val state = _codecState.asStateFlow()

	private val _sampleData = MutableStateFlow<RMSValues>(emptyList())

	@OptIn(ExperimentalCoroutinesApi::class)
	override val wavefront: Flow<RMSValues>
		get() = _sampleData.filter(RMSValues::isNotEmpty)
			.mapLatest { samples -> samples.paddedList(_expectedPoints) }
			.distinctUntilChanged()
			.map { it.normalize() }

	override val isReaderRunning: Flow<Boolean>
		get() = _codecState.map { it == MediaCodecState.EXEC }
			.distinctUntilChanged()


	override suspend fun performWaveformsReading(audioId: Long): Resource<Unit, Exception> {
		return try {
			// clearing the values
			resetAll()
			// get the audio uri
			val audioUri = fileProvider.providesAudioFileUri(audioId)
			Log.d(TAG, "EVALUATING FOR URI: $audioUri")
			// start media codec
			startMediaDecoder(audioUri)
			Resource.Success(Unit)
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e)
		}

	}

	private val codecCallback = object : MediaCodec.Callback() {

		override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
			try {
				if (_codecState.value != MediaCodecState.EXEC) {
					Log.d(TAG, "CANNOT RUN IN WRONG STATE OR END OF BUFFER REACHED")
					return
				}
				val extractor1 = extractor ?: return
				if (index < 0) return
				// get the input buffer
				val inputBuffer = codec.getInputBuffer(index) ?: return
				// read the sample data and store it in the buffer
				val sampleSize = extractor1.readSampleData(inputBuffer, 0)
				if (sampleSize < 0) {
					//no data so end of stream
					codec.signalEndOfInputStream()
					Log.d(TAG, "END OF INPUT STREAM")
					_codecState.update { MediaCodecState.END }
				} else {
					// enqueue the buffer with the buffer info
					codec.queueInputBuffer(
						index, 0, sampleSize, extractor1.sampleTime, 0
					)
					//advance the extractor to read the next sample
					val advance = extractor1.advance()
					if (!advance) {
						Log.d(TAG, "CANNOT ADVANCE EXTRACTOR ANY MORE")
						_codecState.update { MediaCodecState.END }
					}
				}
			} catch (e: IllegalStateException) {
				Log.e(TAG, "MEDIA CODEC IS NOT IN EXECUTION STATE")
			}
		}

		override fun onOutputBufferAvailable(
			codec: MediaCodec,
			index: Int,
			info: MediaCodec.BufferInfo,
		) {
			try {
				if (_codecState.value != MediaCodecState.EXEC) {
					Log.d(TAG, "CODEC IS NOT IN EXEC STATE")
					return
				}
				// stop if end of buffer is reached
				if (info.isEof) {
					Log.d(TAG, "END OF BUFFER REACHED STOPPING CODEC")
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
			Log.d(TAG, "EVALUATING META DATA FOR THE FILE")

			_channels = format.channels
			_pcmEncodingBit = format.pcmEncoding
			val sampleRate = format.sampleRate

			val durationInSeconds = _audioDuration.toInt(DurationUnit.SECONDS)

			val totalPoints = sampleRate * durationInSeconds
			if (_expectedPoints == 0) {
				Log.d(TAG, "EXPECTED POINTS IS NOT SET")
				return
			}
			_perSamplePoints = totalPoints / _expectedPoints
			Log.d(TAG, "POINTS :$_expectedPoints ,PER SAMPLE POINTS  :$_perSamplePoints")
		}
	}


	private suspend fun startMediaDecoder(uri: Uri) {
		withContext(Dispatchers.Default) {
			extractor = MediaExtractor().apply {
				setDataSource(context, uri, null)
			}
			val format = extractor?.getTrackFormat(0) ?: return@withContext

			val mimeType = format.mimeType
			_audioDuration = format.duration

			if (mimeType == null) {
				Log.d(TAG, "AMPLITUDE READER")
				throw InvalidMimeTypeException()
			}

			_expectedPoints = _audioDuration.toInt(DurationUnit.MILLISECONDS)
				.div(VoiceRecorder.RECORDER_AMPLITUDES_BUFFER_SIZE)

			Log.d(TAG, "MIME TYPE : $mimeType, DURATION: $_audioDuration")
			// track selected
			extractor?.selectTrack(0)
			Log.d(TAG, "TRACK SELECTED")
			// creates the decoder
			try {
				// preparing the media codec
				_codecState.update { MediaCodecState.STOP }
				// reset the mediacodec
				mediaCodec?.reset()
				// configure the decoder
				mediaCodec = MediaCodec.createDecoderByType(mimeType).apply {
					configure(format, null, null, 0)
					setCallback(codecCallback)
				}
				//start the decoder
				mediaCodec?.start()
				_codecState.update { MediaCodecState.EXEC }
				Log.d(TAG, "MEDIA CODEC STARTED SUCCESSFULLY")
			} catch (e: IllegalStateException) {
				Log.e(TAG, "ILLEGAL STATE FOUND PLEASE CHECK", e)
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}

	@Synchronized
	private fun evaluateRms(value: Float, partitions: Int) {
		val count = _sampleCount.incrementAndGet()
		if (count == _perSamplePoints) {
			// the root-mean-square value of the sample points
			val block = _perSamplePoints / partitions
			val rms: Float = sqrt(_squaredSum / block)
			_sampleData.update { it + rms }
			_sampleCount.set(0)
			_squaredSum = 0.0f
		}
		_squaredSum += value.pow(2)
	}


	private fun ByteBuffer.handle8bit(bufferInfoSize: Int) {
		val partitions = if (_channels == 2) 2 else 1
		val times = bufferInfoSize / partitions
		repeat(times) {
			val result = get().toInt() / TWO_POWER_7
			if (_channels == 2) {
				// skip the next value
				get()
			}
			evaluateRms(result, partitions)
		}
	}

	private fun ByteBuffer.handle16bit(bufferInfoSize: Int) {
		val partitions = if (_channels == 2) 4 else 2
		val times = bufferInfoSize / partitions
		repeat(times) {
			val first = get().toInt()
			val second = get().toInt() shl 8
			val value = (first or second) / TWO_POWER_15
			if (_channels == 2) {
				//skipping the next 2 values
				repeat(2) { get() }
			}
			evaluateRms(value, partitions)
		}
	}

	private fun ByteBuffer.handle32bit(bufferInfoSize: Int) {
		val partitions = if (_channels == 2) 8 else 4
		val times = bufferInfoSize / partitions

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
			evaluateRms(value.toFloat(), partitions)
		}
	}

	private fun RMSValues.normalize(): RMSValues {

		val max = maxOrNull() ?: 0f
		val min = minOrNull() ?: 0f
		val range = max - min
		return if (range <= 0) emptyList()
		else map { amt ->
			((amt - min) / range).coerceIn(0f..1f)
		}

	}

	private fun RMSValues.paddedList(
		length: Int = 1,
		builder: (Int) -> Float = { 0f },
	): RMSValues {
		val extraSize = length - this.size
		return if (extraSize <= 0) this
		else this + List(extraSize, builder)
	}

	private fun resetAll() {
		Log.i(TAG, "RESETTING  MEDIA CODEC")
		mediaCodec?.reset()
		// update the fields
		_sampleData.update { emptyList() }
		_codecState.update { MediaCodecState.END }
		// reinit values  the fields
		_squaredSum = 0f
		_audioDuration = 0.microseconds
		_perSamplePoints = 0
		_sampleCount.set(0)
	}

	override fun clearResources() {
		Log.i(TAG, "RELEASING MEDIA EXTRACTOR")
		extractor?.release()
		extractor = null

		Log.i(TAG, "RELEASING MEDIA CODEC")
		mediaCodec?.release()
		mediaCodec = null
		_codecState.update { MediaCodecState.END }
		// reinstituting the fields
		_squaredSum = 0f
		_audioDuration = 0.microseconds
		_perSamplePoints = 0
		_sampleCount.set(0)
	}
}

private val MediaFormat.pcmEncoding: Int
	get() = if (containsKey(MediaFormat.KEY_PCM_ENCODING)) {
		when (getInteger(MediaFormat.KEY_PCM_ENCODING)) {
			AudioFormat.ENCODING_PCM_8BIT -> 8
			AudioFormat.ENCODING_PCM_16BIT -> 16
			AudioFormat.ENCODING_PCM_32BIT -> 32
			else -> 16
		}
	} else 16

private val MediaFormat.channels: Int
	get() = getInteger(MediaFormat.KEY_CHANNEL_COUNT)

private val MediaFormat.sampleRate: Int
	get() = getInteger(MediaFormat.KEY_SAMPLE_RATE)

private val MediaFormat.duration: Duration
	get() = getLong(MediaFormat.KEY_DURATION).microseconds

private val MediaFormat.mimeType: String?
	get() = getString(MediaFormat.KEY_MIME)