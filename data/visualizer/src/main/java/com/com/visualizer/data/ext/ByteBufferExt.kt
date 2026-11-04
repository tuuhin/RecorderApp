package com.com.visualizer.data.ext

import java.nio.ByteBuffer

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

    val encoding = when (pcmEncoding) {
        8 -> PcmSampleEncoding.UNSIGNED_8
        16 -> PcmSampleEncoding.SIGNED_16
        32 -> PcmSampleEncoding.FLOAT_32
        else -> throw IllegalArgumentException("Unsupported PCM encoding: $pcmEncoding")
    }
    val input = duplicate().order(order())
    var squares = 0.0
    repeat(frames) {
        val mono = input.readMonoFrame(encoding, channelCount)
        squares += mono * mono
    }
    return squares
}

internal fun ByteBuffer.readMonoFrame(encoding: PcmSampleEncoding, channelCount: Int): Double {
    var mono = 0.0
    repeat(channelCount) {
        val sample = when (encoding) {
            PcmSampleEncoding.UNSIGNED_8 -> ((get().toInt() and 0xff) - 128) / 128.0
            PcmSampleEncoding.SIGNED_16 -> short / 32_768.0
            PcmSampleEncoding.SIGNED_24 -> {
                val value = (get().toInt() and 0xff) or
                    ((get().toInt() and 0xff) shl 8) or
                    ((get().toInt() and 0xff) shl 16)
                ((value shl 8) shr 8) / 8_388_608.0
            }

            PcmSampleEncoding.SIGNED_32 -> int / 2_147_483_648.0
            PcmSampleEncoding.FLOAT_32 -> float.toDouble().let {
                if (it.isFinite()) it.coerceIn(-1.0, 1.0) else 0.0
            }
        }
        mono += sample
    }
    return mono / channelCount
}
