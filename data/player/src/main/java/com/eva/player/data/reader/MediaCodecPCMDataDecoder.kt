package com.eva.player.data.reader

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Handler
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.plusAssign
import kotlin.math.sqrt
import kotlin.time.Duration

private const val TAG = "CODEC_CALLBACK"
private const val PROCESSING_TAG = "CODEC_PROCESSING"

@OptIn(ExperimentalAtomicApi::class)
internal class MediaCodecPCMDataDecoder(
	private val seekDurationMillis: Int,
	private val totalTime: Duration,
	private val extractor: MediaExtractor,
) : MediaCodec.Callback() {

	@Volatile
	private var _mediaCodec: MediaCodec? = null

	@Volatile
	private var _codecState = MediaCodecState.STOPPED

	private val _mutex = Mutex()
	private val _scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
	private val _operations = ConcurrentLinkedQueue<Deferred<Float>>()

	// callbacks
	private var _onBufferDecoded: ((FloatArray) -> Unit)? = null
	private var _onDecodeComplete: (() -> Unit)? = null

	// need to play with the size to get the optimal results
	private val batchSize = 50

	private val currentTimeInMs = AtomicLong(0L)
	private val _isBatchProcessing = AtomicBoolean(false)
	private var _isCleaningUp = AtomicBoolean(false)

	override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
		// clean up is called thus no more processing
		if (_isCleaningUp.load()) {
			Log.i(TAG, "IGNORING READING INPUT CALLBACK")
			return
		}
		try {
			// codec state is non exec means work is done return END_OF_STREAM
			if (_codecState != MediaCodecState.EXEC) {
				Log.d(TAG, "WRONG CODEC STATE OR END OF BUFFER")
				codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
				return
			}
			val currentTime = currentTimeInMs.load()
			// if timeInMs is greater than totalTime+extra return END_OF_STREAM
			if (currentTime >= totalTime.inWholeMilliseconds + seekDurationMillis) {
				Log.d(TAG, "TOTAL TIME HAS REACHED SENDING END OF STREAM")
				codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
				return
			}
		} catch (e: Exception) {
			Log.e(TAG, "UNABLE TO SEND EOS FLAG", e)
		}

		try {
			// receive a buffer
			if (index < 0) return
			val inputBuffer = codec.getInputBuffer(index) ?: return

			// seek the extractor as we don't need extra data
			extractor.seekTo(currentTimeInMs.load() * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
			val sampleSize = extractor.readSampleData(inputBuffer, 0)

			// sample size is zero thus processing done END_OF_STREAM
			if (sampleSize <= 0) {
				_codecState = MediaCodecState.RELEASED
				Log.d(TAG, "END OF INPUT STREAM")
				codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
				return
			}

			// advance the extractor to read the next sample if not END_OF_STREAM
			if (!extractor.advance()) {
				Log.d(TAG, "CANNOT ADVANCE EXTRACTOR ANY MORE")
				_codecState = MediaCodecState.RELEASED
				codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
			} else if (_codecState == MediaCodecState.EXEC) {
				// update the current time
				currentTimeInMs.plusAssign(seekDurationMillis.toLong())
				codec.queueInputBuffer(index, 0, sampleSize, extractor.sampleTime, 0)
			}

		} catch (e: MediaCodec.CodecException) {
			Log.e(TAG, "MEDIA CODEC EXCEPTION ${e.diagnosticInfo} ${e.errorCode} ", e)
		} catch (e: IllegalStateException) {
			Log.e(TAG, "MEDIA CODEC IS NOT IN EXECUTION STATE", e)
		}
	}


	override fun onOutputBufferAvailable(
		codec: MediaCodec,
		index: Int,
		info: MediaCodec.BufferInfo
	) {
		if (_isCleaningUp.load()) {
			Log.i(TAG, "IGNORING PROCESSING OUTPUT CALLBACK")
			return
		}
		if (_codecState != MediaCodecState.EXEC) {
			Log.d(TAG, "WRONG STATE CANNOT PROCESS SHOULD BE EXEC")
			return
		}
		if (info.isEndOfStream) {
			codec.stop()
			_codecState = MediaCodecState.RELEASED
			handleEndOfStream()
			return
		}
		if (info.size > 0) {
			try {
				if (_codecState != MediaCodecState.EXEC) return
				val outputBuffer = codec.getOutputBuffer(index) ?: return
				outputBuffer.position(info.offset)
				outputBuffer.rewind()
				outputBuffer.order(ByteOrder.LITTLE_ENDIAN)

				val format = codec.outputFormat
				val pcmEncoding = format.pcmEncoding
				val channelCount = format.channels
				val floatArray = outputBuffer.asFloatArray(info.size, pcmEncoding, channelCount)
				handleFloatArray(floatArray)

				// release the buffer as work is done
				if (_codecState == MediaCodecState.EXEC)
					codec.releaseOutputBuffer(index, false)
			} catch (e: IllegalStateException) {
				Log.e(TAG, "WRONG STATE TO HANDLE THE OUTPUT BUFFERS", e)
			}
		}
	}

	override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
		if (e.isRecoverable) codec.stop()

		Log.e(TAG, "ERROR HAPPENED", e)
	}

	override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
		Log.d(TAG, "MEDIA FORMAT CHANGED: $format")
	}

	private fun handleFloatArray(pcm: FloatArray) {
		// if there is some pcm data available
		if (pcm.isNotEmpty()) {
			val action = _scope.async { pcm.performRMS() }
			_operations.offer(action)
		}

		// Try to acquire the lock atomically BEFORE launching
		if (!_isBatchProcessing.compareAndSet(expectedValue = false, newValue = true)) return

		_scope.launch {
			try {
				if (_operations.size >= batchSize && isActive) {
					// batched operation
					_isBatchProcessing.compareAndSet(expectedValue = false, newValue = true)
					val operations = buildSet {
						repeat(batchSize) {
							val item = _operations.poll() ?: return@repeat
							add(item)
						}
					}
					Log.i(PROCESSING_TAG, "EVALUATING INFORMATION BATCH :${operations.size}")
					val resultAsFloatArray = operations.awaitAll().toFloatArray()
					_onBufferDecoded?.invoke(resultAsFloatArray)
				}
			} catch (_: CancellationException) {
				Log.d(PROCESSING_TAG, "CANCELLATION IN BATCH PROCESSING")
			} catch (e: Exception) {
				Log.d(PROCESSING_TAG, "Exception at the end of stream", e)
			} finally {
				_isBatchProcessing.store(false)
			}
		}
	}

	private fun handleEndOfStream() {
		_scope.launch {
			try {
				Log.d(PROCESSING_TAG, "END OF BUFFER REACHED, AWAITING OPERATIONS")

				val leftItems = buildSet {
					while (isActive) {
						val item = _operations.poll() ?: break
						add(item)
					}
				}
				Log.i(PROCESSING_TAG, "EVALUATING INFORMATION END :${leftItems.size}")
				_mutex.withLock {
					if (leftItems.isEmpty()) return@withLock
					val results = leftItems.awaitAll()
					val resultAsFloatArray = results.toFloatArray()
					_onBufferDecoded?.invoke(resultAsFloatArray)
					_onDecodeComplete?.invoke()
				}
			} catch (e: Exception) {
				if (e is CancellationException) {
					Log.d(PROCESSING_TAG, "CANCELLATION IN HANDLING END OF STREAM")
					return@launch
				}
				Log.d(PROCESSING_TAG, "Exception at the end of stream", e)
			}
		}
	}

	fun setOnBufferDecode(listener: (FloatArray) -> Unit) {
		_onBufferDecoded = listener
	}

	fun setOnComplete(listener: () -> Unit) {
		_onDecodeComplete = listener
	}

	fun initiateCodec(format: MediaFormat, mimeType: String, handler: Handler) {
		_isCleaningUp.store(false)
		_isBatchProcessing.store(false)
		currentTimeInMs.store(0)

		_mediaCodec?.reset()
		_mediaCodec = MediaCodec.createDecoderByType(mimeType).apply {
			setCallback(this@MediaCodecPCMDataDecoder, handler)
			configure(format, null, null, 0)
		}
		Log.i(TAG, "MEDIA CODEC CREATED IN THREAD:${handler.looper.thread.name}")
		_mediaCodec?.start()
		_codecState = MediaCodecState.EXEC
		Log.i(TAG, "MEDIA CODEC STARTED")
	}

	fun cleanUp() {

		Log.d(PROCESSING_TAG, "CANCELLING OPERATIONS")
		_operations.forEach { it.cancel() }
		_operations.clear()

		_scope.cancel()

		val mediaCodec = _mediaCodec ?: return

		try {
			_isCleaningUp.compareAndSet(expectedValue = false, newValue = true)
			if (_codecState == MediaCodecState.EXEC) {
				// codec is stopped then switched to stopped state
				mediaCodec.stop()
				Log.i(TAG, "CODEC STOPPED")
				_codecState = MediaCodecState.STOPPED
			}
		} catch (e: Exception) {
			Log.e(TAG, "UNABLE TO STOP CODEC", e)
		} finally {
			_isCleaningUp.compareAndSet(expectedValue = true, newValue = false)
		}

		// release the codec
		try {
			if (_codecState == MediaCodecState.STOPPED) {
				Log.i(TAG, "CODEC CALLBACK CLEANED")
				mediaCodec.setCallback(null)
			}
			mediaCodec.release()
			Log.i(TAG, "MEDIA CODEC RELEASED")
		} catch (e: Exception) {
			Log.e(TAG, "UNABLE TO RELEASE MEDIA CODEC", e)
		} finally {
			_codecState = MediaCodecState.RELEASED
			_mediaCodec = null
		}
	}

	private suspend fun FloatArray.performRMS(): Float {
		return withContext(Dispatchers.Default) {
			val squaredAvg = map { it * it }.average().toFloat()
			sqrt(squaredAvg)
		}
	}
}