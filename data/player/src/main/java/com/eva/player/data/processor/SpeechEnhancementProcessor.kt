@file:androidx.media3.common.util.UnstableApi

package com.eva.player.data.processor

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import com.eva.speech_enhancements.IClearSpeechEnhancer
import com.eva.speech_enhancements.models.EnhanceOptions
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalAtomicApi::class)
internal class SpeechEnhancementProcessor(
    private val speechEnhancer: IClearSpeechEnhancer,
) : BaseAudioProcessor() {

    private val enhanceOptions: EnhanceOptions = EnhanceOptions.Street
    private val isPrepared = AtomicBoolean(false)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {

        val supported = inputAudioFormat.sampleRate == MODEL_SAMPLE_RATE &&
            inputAudioFormat.channelCount == 1 &&
            (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT || inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT)

        if (!supported) {
            Log.w(TAG, "SPEECH ENHANCEMENT INACTIVE FOR FORMAT $inputAudioFormat")
            return AudioProcessor.AudioFormat.NOT_SET
        }

        if (!isPrepared.load()) {
            try {
                runBlocking { speechEnhancer.prepare() }
            } catch (e: Exception) {
                Log.e(TAG, "FAILED TO PREPARE THE SPEECH ENHANCER, FALLING BACK TO PASSTHROUGH", e)
                return AudioProcessor.AudioFormat.NOT_SET
            } finally {
                if (!isPrepared.compareAndSet(expectedValue = false, true))
                    Log.w(TAG, "PREPARE SHOULD BE FALSE BUT FOUND TRUE")

            }
        }
        Log.i(TAG, "SPEECH ENHANCEMENT ACTIVE FOR FORMAT $inputAudioFormat")
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!isActive) {
            Log.w(TAG, "INPUT BUFFER NOT READY")
            // the input buffer is not active
            inputBuffer.position(inputBuffer.limit())
            return
        }

        val remaining = inputBuffer.remaining()
        if (remaining <= 0) return

        // Decode straight from the processor buffer: this avoids an intermediate bytearray for
        // every clip of incoming buffer
        val mono = inputBuffer.decodeToMono(inputAudioFormat.encoding, inputAudioFormat.channelCount)
        // The processor owns the input buffer; mark it fully consumed.
        inputBuffer.position(inputBuffer.limit())

        if (mono.isEmpty()) return

        val enhancedResults = try {
            runBlocking { speechEnhancer.process(mono, enhanceOptions) }
        } catch (e: Exception) {
            // on failure keep the (downmixed) input so playback carries on without gaps
            Log.e(TAG, "SPEECH ENHANCEMENT PROCESS FAILED, PASSTHROUGH THIS CHUNK FALLBACK ORIGINAL", e)
            mono
        }

        writeOutput(enhancedResults)
    }


    override fun onQueueEndOfStream() {
        if (!isActive) {
            Log.w(TAG, "BUFFER IS NOT TO WORK WITH")
            return
        }
        try {
            val results = runBlocking { speechEnhancer.finish(enhanceOptions) }
            writeOutput(results)
        } catch (e: Exception) {
            Log.e(TAG, "SPEECH ENHANCEMENT FINISH FAILED", e)
        }
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        if (!isPrepared.load() || !speechEnhancer.isReady) return

        try {
            // a flush restarts the stream (seek or reconfigure): drop all enhancer state
            runBlocking { speechEnhancer.reset() }
        } catch (e: Exception) {
            Log.e(TAG, "SPEECH ENHANCEMENT RESET ON FLUSH FAILED", e)
        }
    }

    override fun onReset() {
        if (!isPrepared.load() || !speechEnhancer.isReady) return
        try {
            runBlocking { speechEnhancer.reset() }
            // close the stream
            speechEnhancer.close()
        } catch (e: Exception) {
            Log.e(TAG, "SPEECH ENHANCEMENT RESET ON FLUSH FAILED", e)
        } finally {
            isPrepared.compareAndSet(expectedValue = true, false)
        }
    }

    private fun writeOutput(samples: FloatArray) {
        if (samples.isEmpty()) return

        val channels = inputAudioFormat.channelCount
        val bytesPerSample = when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT -> 2
            C.ENCODING_PCM_FLOAT -> 4
            else -> return
        }
        if (channels <= 0) return

        // replace the audio buffer with the new size
        val outputBuffer = replaceOutputBuffer(samples.size * channels * bytesPerSample)
            .order(ByteOrder.nativeOrder())

        // update the output buffer based on the pcm encoding
        when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT -> samples.forEach { sample ->
                val pcm = (sample.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
                repeat(channels) { outputBuffer.putShort(pcm) }
            }

            C.ENCODING_PCM_FLOAT -> samples.forEach { sample ->
                repeat(channels) { outputBuffer.putFloat(sample) }
            }

            else -> Log.w(TAG, "INVALID AUDIO ENCODING TO WORK WITH")
        }
        outputBuffer.flip()
    }

    private fun ByteBuffer.decodeToMono(encoding: Int, channels: Int): FloatArray {
        if (channels <= 0) return FloatArray(0)

        val buffer = duplicate().order(ByteOrder.nativeOrder())

        return when (encoding) {
            C.ENCODING_PCM_16BIT -> {
                val samples = buffer.asShortBuffer()
                val frames = samples.remaining() / channels
                FloatArray(frames) { frame ->
                    var sum = 0f
                    for (channel in 0 until channels) {
                        sum += samples.get(frame * channels + channel) / 32768f
                    }
                    sum / channels
                }
            }

            C.ENCODING_PCM_FLOAT -> {
                val samples = buffer.asFloatBuffer()
                val frames = samples.remaining() / channels
                FloatArray(frames) { frame ->
                    var sum = 0f
                    for (channel in 0 until channels) {
                        sum += samples.get(frame * channels + channel)
                    }
                    sum / channels
                }
            }

            else -> FloatArray(0)
        }
    }

    private companion object {
        const val MODEL_SAMPLE_RATE = 48_000
        private const val TAG = "SPEECH_ENHANCEMENT_PROCESSOR"
    }
}
