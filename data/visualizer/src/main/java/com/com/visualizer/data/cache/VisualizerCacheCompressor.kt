package com.com.visualizer.data.cache

import com.eva.compression.LZ4NativeCompression
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object VisualizerCacheCompressor {

    suspend fun encode(points: FloatArray): CompactVisualizerPoints {

        val scale = points.maxOrNull() ?: 0f

        val quantized = ShortArray(points.size)

        if (scale > 0f) {
            for (index in points.indices) {
                val scaledShort = ((points[index] / scale) * Short.MAX_VALUE)
                quantized[index] = scaledShort.roundToInt().coerceIn(0, Short.MAX_VALUE.toInt())
                    .toShort()
            }
        }

        val buffer = ByteBuffer
            .allocate(quantized.size * Short.SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)

        buffer.asShortBuffer().put(quantized)

        val compressionResult = withContext(Dispatchers.Default) { LZ4NativeCompression.compress(buffer.array()) }

        return CompactVisualizerPoints(
            scale = scale,
            pointCount = points.size,
            payload = compressionResult,
        )
    }

    suspend fun decode(proto: VisualizerCacheProtoEntry): FloatArray {
        val quantized = ShortArray(proto.pointCount)

        val results = withContext(Dispatchers.Default) {
            LZ4NativeCompression.decompress(proto.points, proto.pointCount * Short.SIZE_BYTES)
        }

        ByteBuffer
            .wrap(results)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()
            .get(quantized)

        val points = FloatArray(proto.pointCount)

        if (proto.amplitudeScale > 0f) {
            for (index in points.indices) {
                val scaled = quantized[index] * proto.amplitudeScale
                points[index] = scaled / Short.MAX_VALUE
            }
        }

        return points
    }

    internal class CompactVisualizerPoints(
        val scale: Float,
        val pointCount: Int,
        val payload: ByteArray,
    )
}

