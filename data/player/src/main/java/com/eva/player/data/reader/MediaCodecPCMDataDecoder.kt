package com.eva.player.data.reader

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Handler
import android.os.HandlerThread
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

@OptIn(ExperimentalAtomicApi::class)
internal class MediaCodecPCMDataDecoder(
	private val seekDurationMillis: Int,
	private val totalTime: Duration,
	private val extractor: MediaExtractor? = null,
) : MediaCodec.Callback() {

	private val threadName = "MediaCodecComputeThread"
	private var _handlerThread: HandlerThread? = null
	private var _handler: Handler? = null
	private var _mediaCodec: MediaCodec? = null

	private val _scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
	private val _operations = ConcurrentLinkedQueue<Deferred<Float>>()

	@Volatile
	private var _codecState = MediaCodecState.EXEC

	private val _mutex = Mutex()

	// callbacks
	private var _onBufferDecoded: ((FloatArray) -> Unit)? = null
	private var _onDecodeComplete: (() -> Unit)? = null

	// need to play with the size to get the optimal results
	private val batchSize = 50

	private val currentTimeInMs = AtomicLong(0L)
	private val _isBatchProcessing = AtomicBoolean(false)

	override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {

		// codec state is non exec means work is done return END_OF_STREAM
		if (_codecState != MediaCodecState.EXEC) {
			Log.d(TAG, "CANNOT RUN IN WRONG STATE OR END OF BUFFER REACHED")
			codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
			return
		}
		val currentTime = currentTimeInMs.load()
		// if timeInMs is greater than totalTime+extra return END_OF_STREAM
		if (currentTime >= totalTime.inWholeMilliseconds + seekDurationMillis) {
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
			extractor.seekTo(currentTimeInMs.load() * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
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
		if (info.isEndOfStream) {
			Log.i(TAG, "EVERYTHING RAN ON ${Thread.currentThread().name}")
			codec.stop()
			_codecState = MediaCodecState.STOP
			handleEndOfStream()
			return
		}
		if (info.size > 0) {
			try {
				val outputBuffer = codec.getOutputBuffer(index) ?: return
				outputBuffer.position(info.offset)
				outputBuffer.rewind()
				outputBuffer.order(ByteOrder.LITTLE_ENDIAN)

				val format = codec.outputFormat
				val pcmEncoding = format.pcmEncoding
				val channelCount = format.channels
				val floatArray = outputBuffer.asFloatArray(info.size, pcmEncoding, channelCount)
				handleFloatArray(floatArray)
			} catch (e: Exception) {
				e.printStackTrace()
			} finally {
				codec.releaseOutputBuffer(index, false)
			}
		}

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
					Log.i(TAG, "EVALUATING INFORMATION BATCH :${operations.size}")
					val resultAsFloatArray = operations.awaitAll().toFloatArray()
					_onBufferDecoded?.invoke(resultAsFloatArray)
				}
			} catch (_: CancellationException) {
				Log.d(TAG, "CANCELLATION IN BATCH PROCESSING")
			} catch (e: Exception) {
				Log.d(TAG, "Exception at the end of stream", e)
			} finally {
				_isBatchProcessing.store(false)
			}
		}
	}

	private fun handleEndOfStream() {
		_scope.launch {
			try {
				Log.d(TAG, "END OF BUFFER REACHED, AWAITING OPERATIONS")

				val leftItems = buildSet {
					while (isActive) {
						val item = _operations.poll() ?: break
						add(item)
					}
				}
				Log.i(TAG, "EVALUATING INFORMATION END :${leftItems.size}")
				_mutex.withLock {
					if (leftItems.isEmpty()) return@withLock
					val results = leftItems.awaitAll()
					val resultAsFloatArray = results.toFloatArray()
					_onBufferDecoded?.invoke(resultAsFloatArray)
					_onDecodeComplete?.invoke()
				}
			} catch (_: CancellationException) {
				Log.d(TAG, "CANCELLATION IN HANDLING END OF STREAM")
			} catch (e: Exception) {
				Log.d(TAG, "Exception at the end of stream", e)
			}
		}
	}

	override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
		Log.e(TAG, "ERROR HAPPENED", e)
	}

	override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
		Log.d(TAG, "MEDIA FORMAT CHANGED: $format")
	}

	fun setOnBufferDecode(listener: (FloatArray) -> Unit) {
		_onBufferDecoded = listener
	}

	fun setOnComplete(listener: () -> Unit) {
		_onDecodeComplete = listener
	}

	fun initiateCodec(format: MediaFormat, mimeType: String) {
		if (_handlerThread == null || _handlerThread?.isAlive == false) {
			_handlerThread = HandlerThread(threadName).apply { start() }
			_handler = Handler(_handlerThread!!.looper)
		}
		_mediaCodec?.reset()
		_mediaCodec = MediaCodec.createDecoderByType(mimeType).apply {
			configure(format, null, null, 0)
			setCallback(this@MediaCodecPCMDataDecoder, _handler!!)
		}
		_mediaCodec?.start()
		Log.d(TAG, "MEDIA CODEC STARTED")
	}

	fun cleanUp() {
		Log.d(TAG, "MEDIA CODEC RELEASING")
		_mediaCodec?.stop()
		_mediaCodec?.release()
		_mediaCodec = null

		// shut down the thread
		val quit = _handlerThread?.quitSafely() ?: false
		_handlerThread = null
		_handler = null
		Log.d(TAG, "HANDLER THREAD STOPPED:$quit")

		Log.d(TAG, "CLEANING UP OPERATIONS")
		_operations.forEach { it.cancel() }
		_operations.clear()

		Log.d(TAG, "CLEARING UP SCOPE")
		_scope.cancel()
	}

	private suspend fun FloatArray.performRMS(): Float {
		return withContext(Dispatchers.Default) {
			val squaredAvg = map { it * it }.average().toFloat()
			sqrt(squaredAvg)
		}
	}
}