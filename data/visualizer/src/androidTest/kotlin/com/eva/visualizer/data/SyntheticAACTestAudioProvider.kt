package com.eva.visualizer.data

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Creates a small AAC/MP4 file directly on the device for the instrumented
 * tests: [CYCLES] cycles of a [SEGMENT_SECONDS] seconds loud sine followed by
 * [SEGMENT_SECONDS] seconds of silence, so the parsed visualization can be
 * checked against a known envelope.
 *
 * The encoded file is kept in the cache dir of the given [Context] so
 * repeated runs do not pay the encoding cost again.
 */
object SyntheticAACTestAudioProvider {

    private const val SAMPLE_RATE = 44_100
    private const val BIT_RATE = 64_000
    private const val TONE_FREQUENCY = 440.0

    // each cycle is one loud segment followed by one silent segment
    const val SEGMENT_SECONDS = 5
    const val CYCLES = 3

    // total length of the synthetic audio
    val synthetic_duration: Duration = (SEGMENT_SECONDS * 2 * CYCLES).seconds

    private const val TIMEOUT_US = 10_000L

    fun encodeAlternatingLoudSilence(context: Context): File {
        val output = File(context.cacheDir, "visualizer_test_audio.m4a")
        if (output.exists()) return output

        encode(output, SAMPLE_RATE, channels = 1, generateAlternatingLoudSilence())
        return output
    }


    private fun encode(destination: File, sampleRate: Int, channels: Int, pcm: ShortArray) {
        val format = MediaFormat
            .createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels)
            .apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            }

        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()

        // encode into a temp file first so an interrupted run can never
        // leave a broken file behind that looks like a valid cache entry
        val temp = File(destination.parentFile, destination.name + ".tmp")
        if (temp.exists()) temp.delete()

        val muxer = MediaMuxer(temp.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        try {
            encoderLoop(encoder, muxer, pcm, sampleRate, channels)
            check(temp.renameTo(destination)) { "could not move encoded file into place" }
        } finally {
            try {
                encoder.stop()
            } catch (_: Exception) {
                // already stopped or never started, nothing to release there
            }
            encoder.release()
            muxer.release()
        }
    }

    private fun encoderLoop(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        pcm: ShortArray,
        sampleRate: Int,
        channels: Int,
    ) {
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
                    val chunkShorts = min(buffer.remaining() / 2, pcm.size - inputOffset)

                    if (chunkShorts <= 0) {
                        encoder.queueInputBuffer(
                            inputIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                        )
                        inputDone = true
                    } else {
                        for (i in 0 until chunkShorts) {
                            buffer.putShort(pcm[inputOffset + i])
                        }
                        encoder.queueInputBuffer(inputIndex, 0, chunkShorts * 2, presentationTimeUs, 0)
                        inputOffset += chunkShorts
                        val frames = chunkShorts / channels
                        presentationTimeUs += frames * 1_000_000L / sampleRate
                    }
                }
            }

            val outputIndex = encoder.dequeueOutputBuffer(info, TIMEOUT_US)
            when {
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    check(!muxerStarted) { "output format changed twice" }
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

        check(muxerStarted) { "encoder produced no output" }
        muxer.stop()
    }


    private fun generateAlternatingLoudSilence(): ShortArray {
        val samplesPerSegment = SAMPLE_RATE * SEGMENT_SECONDS
        val pcm = ShortArray(samplesPerSegment * 2 * CYCLES)

        var index = 0
        repeat(CYCLES) {
            // loud segment, a constant amplitude sine
            for (i in 0 until samplesPerSegment) {
                val angle = 2.0 * PI * TONE_FREQUENCY * i / SAMPLE_RATE
                pcm[index++] = (sin(angle) * 0.9 * Short.MAX_VALUE).toInt().toShort()
            }
            // silent segment
            repeat(samplesPerSegment) { pcm[index++] = 0 }
        }
        return pcm
    }
}
