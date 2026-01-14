package com.eva.recorder.data.recorder

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.eva.recorder.data.hasAudioRecordPermission
import com.eva.recorder.domain.models.RecorderState
import com.eva.recorder.domain.recorder.AudioDataReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.util.concurrent.CancellationException

private const val TAG = "AmplitudeVisualizer"

@SuppressLint("MissingPermission")
internal class AudioRecordAmplitudeReader(
	private val context: Context
) : AudioDataReader {

	@Volatile
	private var _recorder: AudioRecord? = null

	@Volatile
	private var _pcmBufferSize: Int = 0

	// recorder error codes
	private val errorCodes = arrayOf(
		AudioRecord.ERROR_INVALID_OPERATION,
		AudioRecord.ERROR_BAD_VALUE,
		AudioRecord.ERROR
	)

	override fun initiateRecorder() {
		if (!context.hasAudioRecordPermission) {
			Log.d(TAG, "MISSING PERMISSION")
			return
		}

		if (_recorder != null) {
			Log.d(TAG, "RECORDER ALREADY INITIATED")
			return
		}
		// don't change the sample rate
		val sampleRate = 16_000
		// encoding is based to 16 bits
		val audioFormat = AudioFormat.ENCODING_PCM_16BIT
		val channelConfig = AudioFormat.CHANNEL_IN_MONO
		val channelCount = 1
		val bytesPerSample = 2

		try {
			val bufferSize = AudioRecord
				.getMinBufferSize(sampleRate, channelConfig, audioFormat)
			if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
				Log.e(TAG, "BUFFER SIZE ERROR : $_pcmBufferSize")
				return
			}

			_pcmBufferSize = bufferSize / (bytesPerSample * channelCount)
			Log.d(TAG, "PCM BUFFER SIZE :$_pcmBufferSize")

			_recorder = AudioRecord(
				MediaRecorder.AudioSource.VOICE_RECOGNITION,
				sampleRate,
				channelConfig,
				audioFormat,
				bufferSize * 2
			)

			if (_recorder?.state != AudioRecord.STATE_INITIALIZED) {
				Log.e(TAG, "AudioRecord initialization failed for visualizer.")
				releaseRecorder() // Ensure cleanup
				return
			}
		} catch (e: IllegalArgumentException) {
			Log.e(TAG, "WRONG ARGUMENTS IN RECORDER", e)
		}
	}

	override fun startRecorder() {
		if (_recorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
			Log.d(TAG, "RECORDER STATE RECORDING CANNOT START AGAIN")
			return
		}
		if (_recorder == null) {
			Log.d(TAG, "RECORDER WAS NOT INITIATED ")
			throw Exception("Audio Record instance not initiated")
		}
		try {
			_recorder?.startRecording()
		} catch (e: IllegalStateException) {
			Log.d(TAG, "WRONG STATE", e)
		}
	}

	override fun stopRecorder() {
		if (_recorder?.recordingState == AudioRecord.RECORDSTATE_STOPPED) {
			Log.d(TAG, "RECORDER STATE IS ALREADY STOPPED")
			return
		}
		try {
			_recorder?.stop()
		} catch (e: IllegalStateException) {
			Log.d(TAG, "WRONG STATE", e)
		}
	}

	override fun releaseRecorder() {
		try {
			if (_recorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
				Log.d(TAG, "AUDIO RECORDER WAS RECORDING STOPPING IT")
				_recorder?.stop()
			}
			// release the audio record obj
			_recorder?.release()
			_recorder = null
			_pcmBufferSize = 0
		} catch (e: Exception) {
			Log.d(TAG, "FAILED TO RELEASE RECORDER", e)
		}
	}

	override fun readRecorderRawBytes(state: RecorderState): Flow<Pair<ShortArray, Int>> =
		channelFlow {

			// reset the state based on the state
			if (!state.canReadAmplitudes) {
				send(shortArrayOf() to -1)
				return@channelFlow
			}
			if (_recorder == null) return@channelFlow
			try {
				val pcmBuffer = ShortArray(_pcmBufferSize)
				var shortsRead: Int

				Log.d(TAG, "READING FRAMES BEGIN")

				while (state == RecorderState.RECORDING && currentCoroutineContext().isActive) {
					// ensure the current coroutine is active otherwise
					shortsRead = _recorder?.read(pcmBuffer, 0, pcmBuffer.size) ?: break
					if (shortsRead in errorCodes || shortsRead == 0) break

					// these are raw bytes
					val bufferCopy = pcmBuffer.copyOf(shortsRead)
					trySend(bufferCopy to shortsRead)
				}
			} catch (e: Exception) {
				if (e is CancellationException) Log.d(TAG, "NO MORE PROCESSING VALUES")
				e.printStackTrace()
			}
		}.flowOn(Dispatchers.IO)
}