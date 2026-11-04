package com.eva.visualizer.data

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaFormat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.com.visualizer.data.PcmWindowAccumulator
import com.com.visualizer.data.consumeCodecOutput
import com.com.visualizer.data.ext.PcmSampleEncoding
import com.com.visualizer.data.ext.resampleRms
import com.com.visualizer.data.ext.sumOfSquaredMonoFrames
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PcmWindowTests {
    @Test
    fun givenUnsignedPcmSilenceWhenMeasuredThenEnergyIsZero() {
        val buffer = ByteBuffer.wrap(byteArrayOf(0x80.toByte(), 0x80.toByte()))

        assertEquals(0.0, buffer.sumOfSquaredMonoFrames(2, 8, 1), 0.000001)
    }

    @Test
    fun givenOffsetAndSplitBuffersWhenAccumulatedThenWindowsStayAligned() {
        val points = mutableListOf<Float>()
        val accumulator = PcmWindowAccumulator(500, points::add)
        val first = pcm16(Short.MIN_VALUE, Short.MAX_VALUE, 0, Short.MAX_VALUE)
        val second = pcm16(0)

        accumulator.append(first, 2, 6, 0, 4, 1, PcmSampleEncoding.SIGNED_16)
        accumulator.append(second, 0, 2, 750_000, 4, 1, PcmSampleEncoding.SIGNED_16)
        accumulator.finish(1_000_000)

        val expected = (Short.MAX_VALUE / 32_768.0) / sqrt(2.0)
        assertEquals(2, points.size)
        points.forEach { assertEquals(expected, it.toDouble(), 0.00001) }
    }

    @Test
    fun givenTimestampGapWhenAccumulatedThenSilenceKeepsItsWindows() {
        val points = mutableListOf<Float>()
        val accumulator = PcmWindowAccumulator(500, points::add)
        val audio = pcm16(Short.MAX_VALUE, Short.MAX_VALUE)

        accumulator.append(audio, 0, 4, 0, 4, 1, PcmSampleEncoding.SIGNED_16)
        accumulator.append(audio, 0, 4, 1_500_000, 4, 1, PcmSampleEncoding.SIGNED_16)
        accumulator.finish(2_000_000)

        assertEquals(4, points.size)
        assertEquals(Short.MAX_VALUE / 32_768.0, points[0].toDouble(), 0.00001)
        assertEquals(0.0, points[1].toDouble(), 0.00001)
        assertEquals(0.0, points[2].toDouble(), 0.00001)
        assertEquals(Short.MAX_VALUE / 32_768.0, points[3].toDouble(), 0.00001)
    }

    @Test
    fun givenFinalShortPayloadWhenFinishedThenItsPartialWindowIsEmitted() {
        val points = mutableListOf<Float>()
        val accumulator = PcmWindowAccumulator(500, points::add)
        val audio = pcm16(Short.MAX_VALUE)

        accumulator.append(audio, 0, 2, 0, 4, 1, PcmSampleEncoding.SIGNED_16)
        accumulator.finish()

        assertEquals(1, points.size)
        assertEquals(Short.MAX_VALUE / 32_768.0, points.single().toDouble(), 0.00001)
    }

    @Test
    fun givenEosBufferWithPayloadWhenProcessedThenFinalSamplesAreIncluded() {
        val points = mutableListOf<Float>()
        val accumulator = PcmWindowAccumulator(500, points::add)
        val buffer = pcm16(Short.MIN_VALUE, Short.MAX_VALUE, Short.MIN_VALUE)
        val info = MediaCodec.BufferInfo().apply {
            set(2, 2, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        }
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_RAW, 4, 1).apply {
            setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
        }

        accumulator.consumeCodecOutput(buffer, info, format)

        assertEquals(1, points.size)
        assertEquals(Short.MAX_VALUE / 32_768.0, points.single().toDouble(), 0.00001)
    }

    @Test
    fun givenSupportedPcmFormatsWhenDecodedThenSamplesUseTheirActualRepresentation() {
        val samples = listOf(
            PcmSampleEncoding.UNSIGNED_8 to byteArrayOf(0),
            PcmSampleEncoding.SIGNED_16 to byteArrayOf(0, 0x80.toByte()),
            PcmSampleEncoding.SIGNED_24 to byteArrayOf(0, 0, 0x80.toByte()),
            PcmSampleEncoding.SIGNED_32 to byteArrayOf(0, 0, 0, 0x80.toByte()),
            PcmSampleEncoding.FLOAT_32 to ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(1f).array(),
        )

        for ((encoding, bytes) in samples) {
            val points = mutableListOf<Float>()
            val accumulator = PcmWindowAccumulator(1_000, points::add)
            accumulator.append(ByteBuffer.wrap(bytes), 0, bytes.size, 0, 1, 1, encoding)
            accumulator.finish()
            assertEquals(1.0, points.single().toDouble(), 0.00001)
        }
    }

    @Test
    fun givenFractionalFrameWindowsWhenBuffersSplitThenNoExtraPointIsCreated() {
        val points = mutableListOf<Float>()
        val accumulator = PcmWindowAccumulator(1, points::add)
        val first = pcm16(*ShortArray(220) { Short.MAX_VALUE })
        val second = pcm16(*ShortArray(221) { Short.MAX_VALUE })

        accumulator.append(first, 0, first.capacity(), 0, 44_100, 1, PcmSampleEncoding.SIGNED_16)
        accumulator.append(
            second,
            0,
            second.capacity(),
            220L * 1_000_000 / 44_100,
            44_100,
            1,
            PcmSampleEncoding.SIGNED_16,
        )
        accumulator.finish()

        assertEquals(10, points.size)
        points.forEach { assertEquals(Short.MAX_VALUE / 32_768.0, it.toDouble(), 0.00001) }
    }

    @Test
    fun givenCachedWindowsWhenIntervalsChangeThenOverlapPreservesTimeline() {
        val cached = floatArrayOf(1f, 0f, 1f)

        assertArrayEquals(floatArrayOf(1f, 1f, 0f, 0f, 1f, 1f), cached.resampleRms(100, 50), 0.00001f)
        val expected = sqrt(2.0 / 3.0).toFloat()
        assertArrayEquals(floatArrayOf(expected, expected), cached.resampleRms(100, 150), 0.00001f)
    }

    private fun pcm16(vararg samples: Short): ByteBuffer =
        ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN).apply {
            for (sample in samples) putShort(sample)
            flip()
        }
}
