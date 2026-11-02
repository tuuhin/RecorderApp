package com.eva.compression

import dalvik.annotation.optimization.FastNative
import java.nio.ByteBuffer
import java.nio.ByteOrder

object LZ4NativeCompression {

    fun compress(input: ByteArray): ByteArray {
        val source = ByteBuffer.allocateDirect(input.size)
            .put(input)
            .flip() as ByteBuffer

        val output = ByteBuffer.allocateDirect(compressionBound(input.size))

        val written = compressBytes(source, output)
        check(written >= 0) { "LZ4 compression failed: $written" }

        output.position(0)
        output.limit(written)

        return ByteArray(written).also(output::get)
    }

    fun decompress(input: ByteArray, expectedSize: Int): ByteArray {
        require(expectedSize >= 0) { "expectedSize must not be negative" }

        val source = ByteBuffer.allocateDirect(input.size)
            .put(input)
            .flip() as ByteBuffer

        val output = ByteBuffer.allocateDirect(expectedSize)

        val written = decompressBytes(source, output)
        check(written >= 0) { "LZ4 decompression failed: $written" }
        check(written == expectedSize) { "Unexpected decompressed size: expected=$expectedSize, actual=$written" }

        output.position(0)
        output.limit(written)

        return ByteArray(written).also(output::get)
    }


    fun compress(input: FloatArray): ByteArray {
        val source =
            ByteBuffer.allocateDirect(Math.multiplyExact(input.size, Float.SIZE_BYTES)).order(ByteOrder.nativeOrder())

        source.asFloatBuffer().put(input)
        source.position(0)
        source.limit(source.capacity())

        val output = ByteBuffer.allocateDirect(
            compressionBound(source.remaining()),
        )

        val written = compressBytes(source, output)
        check(written >= 0) { "LZ4 compression failed: $written" }

        output.position(0)
        output.limit(written)

        return ByteArray(written).also(output::get)
    }

    fun decompressFloats(input: ByteArray, expectedFloatCount: Int): FloatArray {
        require(expectedFloatCount >= 0) {
            "expectedFloatCount must not be negative"
        }

        val expectedBytes = Math.multiplyExact(
            expectedFloatCount,
            Float.SIZE_BYTES,
        )

        val source = ByteBuffer.allocateDirect(input.size)
            .put(input)
            .flip() as ByteBuffer

        val output = ByteBuffer.allocateDirect(expectedBytes)
            .order(ByteOrder.nativeOrder())

        val written = decompressBytes(source, output)
        check(written >= 0) { "LZ4 decompression failed: $written" }
        check(written == expectedBytes) {
            "Unexpected decompressed size: expected=$expectedBytes, actual=$written"
        }

        output.position(0)
        output.limit(written)

        return FloatArray(expectedFloatCount).also {
            output.asFloatBuffer().get(it)
        }
    }

    private fun compressionBound(inputSize: Int): Int {
        require(inputSize >= 0)

        val blocks = (inputSize.toLong() + BLOCK_SIZE - 1) / BLOCK_SIZE
        val bound = inputSize.toLong() + 19L + blocks * 8L

        require(bound <= Int.MAX_VALUE) { "Input is too large to compress" }

        return bound.toInt()
    }

    @FastNative
    private external fun compressBytes(input: ByteBuffer, output: ByteBuffer): Int

    @FastNative
    private external fun decompressBytes(input: ByteBuffer, output: ByteBuffer): Int

    init {
        System.loadLibrary("lz4_compression")
    }

    private const val BLOCK_SIZE = 4 * 1024 * 1024
}
