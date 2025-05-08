package com.eva.editor.data.processor

import android.util.Log
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow

const val TWO_POWER_15 = 32768f

private const val TAG = "GAIN_AUDIO_PROCESSOR"

// TODO: FIX THERE IS SOME ISSUES REMAIN..

@UnstableApi
class GainAudioProcessor(private val gainDb: Float) : AudioProcessor {

	private var _format = AudioProcessor.AudioFormat.NOT_SET
	private var _buffer: ByteBuffer? = null
	private var _outputBuffer: ByteBuffer? = null
	private var _isActive: Boolean = false
	private var _isEnded = false

	override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
		_format = inputAudioFormat
		_isActive = true
		Log.d(TAG, "AUDIO FILE IS CONFIGURED :AUDIO FORMAT :${inputAudioFormat} ")
		return _format
	}

	override fun isActive() = _isActive
	override fun isEnded() = _isEnded

	override fun queueInput(inputBuffer: ByteBuffer) {
		_buffer = inputBuffer
	}

	override fun queueEndOfStream() {
		Log.d(TAG, "END OF STREAM REACHED")
		_isEnded = true
	}

	override fun getOutput(): ByteBuffer {
		if (!isActive || _buffer == null) return EMPTY_BUFFER

		val inputBuffer = _buffer!!
		val bytesPerSample = _format.encoding
		val channelCount = _format.channelCount

		val remainingBytes = inputBuffer.remaining()

		val sampleCount = remainingBytes / bytesPerSample / channelCount
		val bufferCapacity = _outputBuffer?.capacity() ?: 0

		val outputBufferSize = sampleCount * bytesPerSample * channelCount
		if (bufferCapacity < outputBufferSize) {
			_outputBuffer = ByteBuffer.allocateDirect(outputBufferSize)
				.order(ByteOrder.nativeOrder())
		} else {
			_outputBuffer?.clear()
		}

		val checkedGain = if (gainDb !in -24f..24f) 0f else gainDb

		val gainMultiplier = 10.0.pow(checkedGain / 20.0).toFloat()

		repeat(sampleCount) {
			repeat(channelCount) {
				val sample = inputBuffer.getShort().toFloat()
				val amplified = sample * gainMultiplier
				val limitAmplification = amplified.coerceIn(-TWO_POWER_15, TWO_POWER_15)
				_outputBuffer?.putShort(limitAmplification.toInt().toShort())
			}
		}

		inputBuffer.clear()
		val resultBuffer = _outputBuffer
		_outputBuffer = EMPTY_BUFFER

		//check if input buffer is empty
		_isEnded = inputBuffer.remaining() == 0
		Log.d(TAG, "PREPARING END BUFFER")
		return resultBuffer ?: EMPTY_BUFFER
	}


	override fun flush() {
		_buffer?.clear()
		_outputBuffer?.clear()

		// set empty buffer
		_buffer = EMPTY_BUFFER
		_outputBuffer = EMPTY_BUFFER

		_isEnded = false
		Log.d(TAG, "BYTE DATA FLUSHED")
	}

	override fun reset() {
		flush()
		_format = AudioProcessor.AudioFormat.NOT_SET
		_isActive = false
		_buffer = null
		_outputBuffer = null
		Log.d(TAG, "AUDIO PROCESSOR WORK DONE")
	}

	companion object {
		private val EMPTY_BUFFER = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
	}
}