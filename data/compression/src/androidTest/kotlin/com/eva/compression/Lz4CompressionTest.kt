package com.eva.compression

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThanOrEqualTo
import kotlin.math.sin
import kotlin.random.Random
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LZ4NativeCompressionTest {

    @Test
    fun byteArray_roundTrip() {
        val original = "Hello from LZ4 on Android".encodeToByteArray()

        val compressed = LZ4NativeCompression.compress(original)
        val decompressed = LZ4NativeCompression.decompress(compressed, original.size)

        assertThat(original).isEqualTo(decompressed)
    }

    @Test
    fun byteArray_emptyInput_roundTrip() {
        val original = byteArrayOf()

        val compressed = LZ4NativeCompression.compress(original)
        val decompressed = LZ4NativeCompression.decompress(compressed, original.size)
        assertThat(original).isEqualTo(decompressed)
    }

    @Test
    fun byteArray_repetitiveData_roundTrip() {
        val original = ByteArray(256 * 1024) { 'A'.code.toByte() }

        val compressed = LZ4NativeCompression.compress(original)
        val decompressed = LZ4NativeCompression.decompress(compressed, original.size)

        assertThat(original).isEqualTo(decompressed)
        assertThat(compressed.size).isLessThanOrEqualTo(original.size)
    }

    @Test
    fun byteArray_randomData_roundTrip() {
        val original = ByteArray(128 * 1024)
        Random(42).nextBytes(original)

        val compressed = LZ4NativeCompression.compress(original)
        val decompressed = LZ4NativeCompression.decompress(compressed, original.size)

        assertThat(original).isEqualTo(decompressed)
    }

    @Test
    fun byteArray_largeInput_roundTrip() {
        val original = ByteArray(8 * 1024 * 1024) { index -> (index % 251).toByte() }

        val compressed = LZ4NativeCompression.compress(original)
        val decompressed = LZ4NativeCompression.decompress(compressed, original.size)

        assertThat(original).isEqualTo(decompressed)
    }

    @Test
    fun byteArray_wrongExpectedSize_throws() {
        val original = ByteArray(1024) { it.toByte() }
        val compressed = LZ4NativeCompression.compress(original)

        assertThrows(IllegalStateException::class.java) {
            LZ4NativeCompression.decompress(compressed, original.size + 1)
        }
    }

    @Test
    fun byteArray_negativeExpectedSize_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            LZ4NativeCompression.decompress(byteArrayOf(1, 2, 3), -1)
        }
    }

    @Test
    fun byteArray_invalidCompressedData_throws() {
        assertThrows(IllegalStateException::class.java) {
            LZ4NativeCompression.decompress(byteArrayOf(1, 2, 3, 4, 5), 100)
        }
    }

    @Test
    fun floatArray_roundTrip() {
        val original = floatArrayOf(
            0.0f,
            1.0f,
            -1.0f,
            0.5f,
            -0.5f,
            Float.MIN_VALUE,
            Float.MAX_VALUE,
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            Float.NaN,
        )

        val compressed = LZ4NativeCompression.compress(original)
        val decompressed = LZ4NativeCompression.decompressFloats(compressed, original.size)

        assertThat(original).isEqualTo(decompressed)

        original.indices.forEach { index ->
            assertThat(
                original[index].toRawBits(),
                "Mismatch at index $index",
            ).isEqualTo(decompressed[index].toRawBits())
        }
    }

    @Test
    fun floatArray_emptyInput_roundTrip() {
        val original = floatArrayOf()

        val compressed = LZ4NativeCompression.compress(original)
        val decompressed = LZ4NativeCompression.decompressFloats(compressed, original.size)
        assertThat(original).isEqualTo(decompressed)
    }

    @Test
    fun floatArray_waveformLikeData_roundTrip() {
        val original = FloatArray(48_000) { index -> sin(index * 0.02).toFloat() }

        val compressed = LZ4NativeCompression.compress(original)
        val decompressed = LZ4NativeCompression.decompressFloats(
            compressed,
            original.size,
        )

        assertThat(original).isEqualTo(decompressed)

        original.indices.forEach { index ->
            assertThat(
                original[index].toRawBits(),
                "Mismatch at index $index",
            ).isEqualTo(decompressed[index].toRawBits())
        }
    }

    @Test
    fun floatArray_randomData_roundTrip() {
        val random = Random(1234)
        val original = FloatArray(16_384) { random.nextFloat() * 2.0f - 1.0f }

        val compressed = LZ4NativeCompression.compress(original)
        val decompressed = LZ4NativeCompression.decompressFloats(
            compressed,
            original.size,
        )

        assertThat(original).isEqualTo(decompressed)

        original.indices.forEach { index ->
            assertThat(
                original[index].toRawBits(),
                "Mismatch at index $index",
            ).isEqualTo(decompressed[index].toRawBits())
        }
    }

    @Test
    fun floatArray_wrongExpectedCount_throws() {
        val original = floatArrayOf(1.0f, 2.0f, 3.0f)
        val compressed = LZ4NativeCompression.compress(original)

        assertThrows(IllegalStateException::class.java) {
            LZ4NativeCompression.decompressFloats(compressed, original.size + 1)
        }
    }

    @Test
    fun floatArray_negativeExpectedCount_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            LZ4NativeCompression.decompressFloats(byteArrayOf(1, 2, 3), -1)
        }
    }
}
