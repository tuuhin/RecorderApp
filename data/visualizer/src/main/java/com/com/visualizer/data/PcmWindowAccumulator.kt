package com.com.visualizer.data

import com.com.visualizer.data.ext.PcmSampleEncoding
import com.com.visualizer.data.ext.readMonoFrame
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

internal class PcmWindowAccumulator(
    timePerPointMs: Int,
    private val onPoint: (Float) -> Unit,
) {

    private val windowUs = timePerPointMs.toLong().also { require(it > 0) } * 1_000.0
    private var windowStartUs = 0.0
    private var windowEndUs = windowUs
    private var positionUs = 0.0
    private var energy = 0.0
    private var finished = false

    fun append(
        buffer: ByteBuffer,
        offset: Int,
        size: Int,
        presentationTimeUs: Long,
        sampleRate: Int,
        channelCount: Int,
        encoding: PcmSampleEncoding,
    ) {
        check(!finished) { "PCM stream has already finished" }
        require(sampleRate > 0 && channelCount > 0)
        require(channelCount <= Int.MAX_VALUE / encoding.bytesPerSample)
        require(offset >= 0 && size >= 0 && offset <= buffer.capacity() && size <= buffer.capacity() - offset)

        val frameSize = encoding.bytesPerSample * channelCount
        require(size % frameSize == 0) { "PCM output contains an incomplete sample frame" }

        if (size == 0) return

        val input = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN).apply {
            clear()
            position(offset)
            limit(offset + size)
        }

        val frameDurationUs = 1_000_000.0 / sampleRate
        val timestampUs = presentationTimeUs.toDouble()
        val startUs = if (abs(timestampUs - positionUs) <= TIMESTAMP_TOLERANCE_US) positionUs else timestampUs
        if (startUs > positionUs) advanceTo(startUs, 0.0)

        repeat(size / frameSize) { frame ->
            val mono = input.readMonoFrame(encoding, channelCount)
            val frameStartUs = startUs + frame * frameDurationUs
            val frameEndUs = startUs + (frame + 1) * frameDurationUs
            if (frameEndUs > positionUs) {
                if (frameStartUs > positionUs) advanceTo(frameStartUs, 0.0)
                advanceTo(frameEndUs, mono * mono)
            }
        }
    }

    fun finish(endTimestampUs: Long? = null) {
        if (finished) return
        if (endTimestampUs != null && endTimestampUs > positionUs + TIMESTAMP_TOLERANCE_US) {
            advanceTo(endTimestampUs.toDouble(), 0.0)
        }
        val elapsedUs = positionUs - windowStartUs
        if (elapsedUs > 0.0) onPoint(sqrt(energy / elapsedUs).toFloat())
        finished = true
    }

    private fun advanceTo(targetUs: Double, sampleEnergy: Double) {
        if (targetUs <= positionUs) return
        while (targetUs >= windowEndUs) {
            energy += sampleEnergy * (windowEndUs - positionUs)
            positionUs = windowEndUs
            onPoint(sqrt(energy / windowUs).toFloat())
            energy = 0.0
            windowStartUs = windowEndUs
            windowEndUs += windowUs
        }
        energy += sampleEnergy * (targetUs - positionUs)
        positionUs = targetUs
    }

    private companion object {
        const val TIMESTAMP_TOLERANCE_US = 2.0
    }
}
