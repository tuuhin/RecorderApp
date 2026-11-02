package com.com.visualizer.data

import java.nio.ByteBuffer

private const val TWO_POWER_15 = 32_768f
private const val TWO_POWER_7 = 128f

/**
 * Number of complete sample frames in [size] bytes of interleaved PCM data,
 * where a frame holds one sample per channel.
 */
fun pcmFrameCount(size: Int, pcmEncoding: Int, channelCount: Int): Int {
    val bytesPerSample = pcmEncoding / 8
    if (bytesPerSample <= 0 || channelCount <= 0) return 0
    return size / (bytesPerSample * channelCount)
}

/**
 * Sum of the squares of the mono-mixed sample frames in [size] bytes of PCM
 * data starting at the current position of this buffer, mixing all channels
 * down to mono.
 *
 * This is the raw energy of the buffer. Windows spanning several buffers are
 * combined by summing the energies of their buffers and dividing by the
 * summed frame count, which is the root-mean-square of the whole window.
 *
 * Runs in a single pass over the data with no intermediate allocations.
 */
fun ByteBuffer.sumOfSquaredMonoFrames(size: Int, pcmEncoding: Int, channelCount: Int): Double {
    val frames = pcmFrameCount(size, pcmEncoding, channelCount)
    if (frames == 0) return 0.0

    var squares = 0.0
    when (pcmEncoding) {
        16 -> {
            val shortBuffer = asShortBuffer()
            for (frame in 0 until frames) {
                var mono = 0f
                for (channel in 0 until channelCount) {
                    mono += shortBuffer.get(frame * channelCount + channel) / TWO_POWER_15
                }
                mono /= channelCount
                squares += (mono * mono).toDouble()
            }
        }

        8 -> {
            val base = position()
            for (frame in 0 until frames) {
                var mono = 0f
                for (channel in 0 until channelCount) {
                    mono += get(base + frame * channelCount + channel) / TWO_POWER_7
                }
                mono /= channelCount
                squares += (mono * mono).toDouble()
            }
        }

        32 -> {
            val floatBuffer = asFloatBuffer()
            for (frame in 0 until frames) {
                var mono = 0f
                for (channel in 0 until channelCount) {
                    mono += floatBuffer.get(frame * channelCount + channel)
                }
                mono /= channelCount
                squares += (mono * mono).toDouble()
            }
        }

        else -> throw IllegalArgumentException("Unsupported PCM encoding: $pcmEncoding")
    }
    return squares
}
