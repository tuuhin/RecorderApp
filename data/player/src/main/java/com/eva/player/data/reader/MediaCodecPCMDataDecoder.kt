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

private const val CODEC_TAG = "CODEC_CALLBACK"
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

	private val _currentTimeInMs = AtomicLong(0L)
	private val _isBatchProcessing = AtomicBoolean(false)
	private var _isCleaningUp = AtomicBoolean(false)

	override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
		// clean up is called thus no more processing
		if (_isCleaningUp.load()) {
			Log.i(CODEC_TAG, "IGNORING READING INPUT CALLBACK")
			return
		}
		try {
			// codec state is non exec means work is done return END_OF_STREAM
			if (_codecState != MediaCodecState.EXEC) {
				Log.d(PROCESSING_TAG, "WRONG CODEC STATE")
				codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
				return
			}
			// if timeInMs is greater than totalTime+extra return END_OF_STREAM
			if (_currentTimeInMs.load() >= totalTime.inWholeMilliseconds + seekDurationMillis) {
				Log.d(PROCESSING_TAG, "TOTAL TIME HAS REACHED SENDING END OF STREAM")
				codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
				return
			}
		} catch (e: Exception) {
			Log.e(PROCESSING_TAG, "UNABLE TO SEND EOS FLAG", e)
		}

		try {
			// receive a buffer
			if (index < 0) return
			val inputBuffer = codec.getInputBuffer(index) ?: return

			// seek the extractor as we don't need extra data
			extractor.seekTo(_currentTimeInMs.load() * 1_000L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
			val sampleSize = extractor.readSampleData(inputBuffer, 0)

			// sample size is zero thus processing done END_OF_STREAM
			if (sampleSize <= 0) {
				codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
				Log.d(PROCESSING_TAG, "INPUT BUFFER :END OF INPUT STREAM")
				return
			}

			// advance the extractor to read the next sample if not END_OF_STREAM
			if (!extractor.advance()) {
				Log.d(PROCESSING_TAG, "CANNOT ADVANCE EXTRACTOR")
				Log.d(PROCESSING_TAG, "INPUT BUFFER :END OF INPUT STREAM")
				codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
			} else if (_codecState == MediaCodecState.EXEC) {
				// update the current time
				_currentTimeInMs.plusAssign(seekDurationMillis.toLong())
				codec.queueInputBuffer(index, 0, sampleSize, extractor.sampleTime, 0)
			}
		} catch (e: MediaCodec.CodecException) {
			Log.e(PROCESSING_TAG, "MEDIA CODEC EXCEPTION ${e.diagnosticInfo} ${e.errorCode} ", e)
		} catch (e: IllegalStateException) {
			Log.e(PROCESSING_TAG, "MEDIA CODEC IS NOT IN EXECUTION STATE", e)
		}
	}


	override fun onOutputBufferAvailable(
		codec: MediaCodec,
		index: Int,
		info: MediaCodec.BufferInfo
	) {
		// look is this cleaning up
		if (_isCleaningUp.load()) {
			Log.i(CODEC_TAG, "IGNORING PROCESSING OUTPUT CALLBACK")
			return
		}
		// correct state or not
		if (_codecState != MediaCodecState.EXEC) {
			Log.d(PROCESSING_TAG, "WRONG STATE SHOULD BE EXEC FOUND :$_codecState")
			return
		}
		// special case to handle end of stream
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
				Log.e(PROCESSING_TAG, "WRONG STATE TO HANDLE THE OUTPUT BUFFERS", e)
			}
		}
	}

	override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
		if (e.isRecoverable) codec.stop()

		Log.e(CODEC_TAG, "ERROR HAPPENED", e)
	}

	override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
		Log.d(CODEC_TAG, "MEDIA FORMAT CHANGED: $format")
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
				_mutex.withLock {
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
				}
			} catch (e: Exception) {
				if (e is CancellationException)
					Log.d(PROCESSING_TAG, "CANCELLATION IN BATCH PROCESSING")
				Log.d(PROCESSING_TAG, "Exception at the end of stream", e)
			} finally {
				_isBatchProcessing.store(false)
			}
		}
	}

	private fun handleEndOfStream() {
		Log.d(PROCESSING_TAG, "END OF BUFFER REACHED, AWAITING OPERATIONS")
		_scope.launch {
			try {
				_mutex.withLock {
					val leftItems = buildSet {
						while (isActive) {
							val item = _operations.poll() ?: break
							add(item)
						}
					}
					Log.i(PROCESSING_TAG, "EVALUATING INFORMATION END :${leftItems.size}")
					if (leftItems.isEmpty()) return@withLock
					val results = leftItems.awaitAll()
					val resultAsFloatArray = results.toFloatArray()
					_onBufferDecoded?.invoke(resultAsFloatArray)
				}
			} catch (e: Exception) {
				if (e is CancellationException) {
					Log.d(PROCESSING_TAG, "CANCELLATION IN HANDLING END OF STREAM")
					return@launch
				}
				Log.d(PROCESSING_TAG, "Exception at the end of stream", e)
			} finally {
				// stream read ended
				_onDecodeComplete?.invoke()
			}
		}
	}

	fun setOnBufferDecode(listener: (FloatArray) -> Unit) {
		_onBufferDecoded = listener
	}

	fun setOnComplete(listener: () -> Unit) {
		_onDecodeComplete = listener
	}

	@Synchronized
	fun initiateCodec(format: MediaFormat, mimeType: String, handler: Handler) {
		_isCleaningUp.store(false)
		_isBatchProcessing.store(false)
		_currentTimeInMs.store(0)

		_mediaCodec?.reset()
		_mediaCodec = MediaCodec.createDecoderByType(mimeType).apply {
			setCallback(this@MediaCodecPCMDataDecoder, handler)
			configure(format, null, null, 0)
		}
		Log.i(CODEC_TAG, "MEDIA CODEC CREATED IN THREAD:${handler.looper.thread.name}")
		_mediaCodec?.start()
		_codecState = MediaCodecState.EXEC
		Log.i(CODEC_TAG, "MEDIA CODEC STARTED")
	}

	@Synchronized
	fun cleanUp() {
		if (_operations.isNotEmpty()) {
			Log.d(PROCESSING_TAG, "CANCELLING DEFERRED CALCULATIONS")
			_operations.forEach { it.cancel() }
			_operations.clear()
		}
		Log.d(PROCESSING_TAG, "CANCELLING OPERATIONS SCOPE")
		_scope.cancel()

		val mediaCodec = _mediaCodec ?: return

		try {
			_isCleaningUp.compareAndSet(expectedValue = false, newValue = true)
			if (_codecState == MediaCodecState.EXEC) {
				// flush all the buffers
				mediaCodec.flush()
				// codec is stopped then switched to stopped state
				mediaCodec.stop()
				Thread.sleep(10)
				Log.i(CODEC_TAG, "CODEC STOPPED")
				_codecState = MediaCodecState.STOPPED
			}
		} catch (e: Exception) {
			Log.e(CODEC_TAG, "UNABLE TO STOP CODEC", e)
		} finally {
			_isCleaningUp.compareAndSet(expectedValue = true, newValue = false)
		}

		// release the codec
		try {
			if (_codecState == MediaCodecState.STOPPED) {
				Log.i(CODEC_TAG, "CODEC CALLBACK CLEANED")
				mediaCodec.setCallback(null)
			}
			mediaCodec.release()
			Log.i(CODEC_TAG, "MEDIA CODEC RELEASED")
		} catch (e: Exception) {
			Log.e(CODEC_TAG, "UNABLE TO RELEASE MEDIA CODEC", e)
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