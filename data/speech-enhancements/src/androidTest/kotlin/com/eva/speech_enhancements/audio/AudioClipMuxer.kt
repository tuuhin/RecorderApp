package com.eva.speech_enhancements.audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AudioClipMuxer {

    private const val BIT_RATE = 128_000
    private const val TIMEOUT_US = 10_000L

    suspend fun encodeAac(destination: File, samples: FloatArray, sampleRate: Int): File {
        if (samples.isEmpty()) throw IllegalArgumentException("Samples cannot be empty")

        val temp = File(destination.parentFile, destination.name + ".tmp")
        if (temp.exists()) temp.delete()

        val format = MediaFormat
            .createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 1)
            .apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            }

        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()

        val muxer = MediaMuxer(temp.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        try {
            withContext(Dispatchers.Default) {
                encoderLoop(encoder, muxer, samples, sampleRate)
            }
            check(temp.renameTo(destination)) { "could not move encoded clip into place" }
        } finally {
            try {
                encoder.stop()
            } catch (_: Exception) {
                // already stopped or never started; do not mask the original failure
            }
            encoder.release()
            muxer.release()
        }
        return destination
    }

    private fun encoderLoop(encoder: MediaCodec, muxer: MediaMuxer, pcm: FloatArray, sampleRate: Int) {
        var trackIndex = -1
        var muxerStarted = false
        var inputOffset = 0
        var presentationTimeUs = 0L
        var inputDone = false
        var outputDone = false

        val info = MediaCodec.BufferInfo()
        while (!outputDone) {
            if (!inputDone) {

                val inputIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
                if (inputIndex >= 0) {

                    val buffer = encoder.getInputBuffer(inputIndex) ?: continue
                    buffer.clear()
                    val capacityShorts = buffer.remaining() / 2
                    val chunkShorts = min(capacityShorts, pcm.size - inputOffset)

                    if (chunkShorts <= 0) {
                        encoder.queueInputBuffer(
                            inputIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                        )
                        inputDone = true
                    } else {
                        for (i in 0..<chunkShorts) {
                            val clamped = pcm[inputOffset + i].coerceIn(-1.0f, 1.0f)
                            // convert the clamped buffer into short buffer
                            buffer.putShort((clamped * Short.MAX_VALUE).toInt().toShort())
                        }
                        encoder.queueInputBuffer(inputIndex, 0, chunkShorts * 2, presentationTimeUs, 0)
                        inputOffset += chunkShorts
                        presentationTimeUs += chunkShorts * 1_000_000L / sampleRate
                    }
                }
            }

            val outputIndex = encoder.dequeueOutputBuffer(info, TIMEOUT_US)
            when {
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (muxerStarted) throw IllegalStateException("Muxer state should not be changed")
                    // add the track to muxer and start the muxer
                    trackIndex = muxer.addTrack(encoder.outputFormat)
                    muxer.start()
                    muxerStarted = true
                }

                outputIndex >= 0 -> {
                    val encoded = encoder.getOutputBuffer(outputIndex) ?: continue
                    // codec specific data is carried by the output format
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && muxerStarted) {
                        encoded.position(info.offset)
                        encoded.limit(info.offset + info.size)
                        muxer.writeSampleData(trackIndex, encoded, info)
                    }
                    encoder.releaseOutputBuffer(outputIndex, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                }
            }
        }

        if (!muxerStarted) throw IllegalStateException("Muxer should have started")
        muxer.stop()
    }
}
