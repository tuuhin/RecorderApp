package com.eva.player.data.reader

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import androidx.collection.mutableScatterSetOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.time.Duration

private const val TAG = "CODEC_CALLBACK"

class MediaCodecPCMDataDecoder(
	private val seekDuration: Int,
	private val totalTime: Duration,
	private val scope: CoroutineScope,
	private val extractor: MediaExtractor? = null,
	private val onBufferDecoded: (FloatArray) -> Unit
) : MediaCodec.Callback() {

	private val _operations = mutableScatterSetOf<Deferred<Float>>()
	private var _codecState = MediaCodecState.EXEC

	private var _mediaCodec: MediaCodec? = null

	private val lock = Any()
	private val _mutex = Mutex()
	private val batchSize = 100

	private var currentTimeInMs = 0L

	override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {

		// codec state is non exec means work is done return END_OF_STREAM
		if (_codecState != MediaCodecState.EXEC) {
			Log.d(TAG, "CANNOT RUN IN WRONG STATE OR END OF BUFFER REACHED")
			codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
			return
		}

		// if timeInMs is greater than totalTime+extra return END_OF_STREAM
		if (currentTimeInMs >= totalTime.inWholeMilliseconds + seekDuration) {
			Log.d(TAG, "TOTAL TIME IS ALREADY REACHED")
			codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
			return
		}

		try {

			// receive a buffer
			if (index < 0) return
			val inputBuffer = codec.getInputBuffer(index) ?: return

			val extractor = extractor ?: return
			// seek the extractor as we don't need extra data
			extractor.seekTo(currentTimeInMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
			val sampleSize = extractor.readSampleData(inputBuffer, 0)

			// sample size is zero thus processing done END_OF_STREAM
			if (sampleSize <= 0) {
				_codecState = MediaCodecState.END
				Log.d(TAG, "END OF INPUT STREAM")
				codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
				return
			}

			// advance the extractor to read the next sample if not END_OF_STREAM
			if (!extractor.advance()) {
				Log.d(TAG, "CANNOT ADVANCE EXTRACTOR ANY MORE")
				_codecState = MediaCodecState.END
				codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
			} else {
				// update the current time
				currentTimeInMs += seekDuration
				codec.queueInputBuffer(index, 0, sampleSize, extractor.sampleTime, 0)
			}

		} catch (e: MediaCodec.CodecException) {
			Log.e(TAG, "MEDIA CODEC EXCEPTION ${e.diagnosticInfo} ${e.errorCode} ", e)
		} catch (e: IllegalStateException) {
			Log.e(TAG, "MEDIA CODEC IS NOT IN EXECUTION STATE", e)
		}
	}


	override fun onOutputBufferAvailable(
		codec: MediaCodec, index: Int,
		info: MediaCodec.BufferInfo
	) {
		try {
			// there is some data
			if (info.size > 0) {
				val outputBuffer = codec.getOutputBuffer(index)
				outputBuffer?.position(info.offset)
				outputBuffer?.rewind()
				outputBuffer?.order(ByteOrder.LITTLE_ENDIAN)

				val format = codec.outputFormat
				val pcmEncoding = format.pcmEncoding
				val channelCount = format.channels
				val sampleRate = format.sampleRate

				val pcm = outputBuffer?.asFloatArray(info.size, pcmEncoding, channelCount)
					?: floatArrayOf()

				if (!scope.isActive) {
					Log.i(TAG, "SCOPE IS NOT ACTIVE ANY MORE STOPING CODEC")
					_codecState = MediaCodecState.STOP
					// release the buffer
					codec.releaseOutputBuffer(index, false)
					return
				}
				// if there is some pcm data available
				if (pcm.isNotEmpty()) {
					val action = scope.async(Dispatchers.Default) {
						performOperation(pcm, sampleRate, seekDuration)
					}
					_operations.plusAssign(action)
				}
				// batched operation
				if (_operations.size >= batchSize && scope.isActive) {
					scope.launch(Dispatchers.Default) {
						if (_mutex.holdsLock(lock)) {
							Log.d(TAG, "A BATCH BEING SEND CANNOT SEND IN THIS BATCH")
							return@launch
						}
						_mutex.lock(lock)
						try {
							Log.i(TAG, "EVALUATING INFORMATION")
							val results = _operations.asSet().awaitAll()
							val resultAsFloatArray = results.toFloatArray()
							onBufferDecoded(resultAsFloatArray)
							_operations.clear()
						} finally {
							_mutex.unlock()
						}
					}
				}
				// release the buffer
				codec.releaseOutputBuffer(index, false)
				return
			}

			if (info.isEndOfStream) {
				Log.d(TAG, "END OF BUFFER REACHED, AWAITING OPERATIONS")
				codec.stop()
				_codecState = MediaCodecState.STOP

				scope.launch(Dispatchers.Default) {
					if (!isActive) return@launch
					Log.i(TAG, "EVALUATING INFORMATION END")


					val results = _operations.asSet().awaitAll()
					val resultAsFloatArray = results.toFloatArray()
					onBufferDecoded(resultAsFloatArray)

				}
			}
		} catch (e: IllegalStateException) {
			Log.e(TAG, "MEDIA CODEC IS NOT IN EXECUTION STATE", e)
		}
	}

	override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
		Log.e(TAG, "ERROR HAPPENED", e)
	}


	override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
		Log.d(TAG, "MEDIA FORMAT CHANGED: $format")
	}

	fun initiateCodec(format: MediaFormat, mimeType: String) {
		_mediaCodec?.reset()
		_mediaCodec = MediaCodec.createDecoderByType(mimeType).apply {
			configure(format, null, null, 0)
			setCallback(this@MediaCodecPCMDataDecoder)
		}
		_mediaCodec?.start()
		Log.d(TAG, "MEDIA CODEC STARTED")
	}

	fun cleanUp() {
		Log.d(TAG, "CLEANING UP OPERATIONS")
		_operations.forEach { it.cancel() }
		_operations.clear()

		Log.d(TAG, "MEDIA CODEC RELEASING")
		_mediaCodec?.stop()
		_mediaCodec?.release()
		_mediaCodec = null
	}

	private suspend fun performOperation(
		samples: FloatArray,
		sampleRate: Int,
		timerPerPoints: Int
	): Float {
		return withContext(Dispatchers.Default) {
			val cutOffFrequency = 1000f / timerPerPoints
			val filteredSamples = samples.lowPassFilter(
				sampleRate = sampleRate,
				cutoffFrequency = cutOffFrequency
			)
			val squaredSum = samples.sumOf { it.toDouble().pow(2) }
			sqrt(squaredSum / samples.size).toFloat()
		}
	}
}