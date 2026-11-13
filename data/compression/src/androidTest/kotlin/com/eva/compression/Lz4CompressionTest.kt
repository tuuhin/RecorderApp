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
    fun byteArray_allZeroData_roundTrip() {
        val original = ByteArray(4096)

        val compressed = LZ4NativeCompression.compress(original)
        val decompressed = LZ4NativeCompression.decompress(compressed, original.size)

        assertThat(original).isEqualTo(decompressed)
    }

    @Test
    fun byteArray_boundarySizes_roundTrip() {
        listOf(1, 3, 4, 7, 8, 15, 16, 63, 64, 255, 256, 1023, 1024).forEach { size ->
            val original = ByteArray(size) { index -> (index * 31).toByte() }
            val compressed = LZ4NativeCompression.compress(original)

            assertThat(LZ4NativeCompression.decompress(compressed, size)).isEqualTo(original)
        }
    }

    @Test
    fun byteArray_truncatedFrame_throws() {
        val compressed = LZ4NativeCompression.compress(ByteArray(1024) { it.toByte() })

        assertThrows(RuntimeException::class.java) {
            LZ4NativeCompression.decompress(compressed.copyOf(compressed.size - 1), 1024)
        }
    }

    @Test
    fun byteArray_trailingData_throws() {
        val compressed = LZ4NativeCompression.compress(byteArrayOf(1, 2, 3))

        assertThrows(RuntimeException::class.java) {
            LZ4NativeCompression.decompress(compressed + byteArrayOf(0), 3)
        }
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
        assertThrows(RuntimeException::class.java) {
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
    fun floatArray_deterministicIncompressibleData_roundTrip() {
        val random = Random(5678)
        val original = FloatArray(8192) { random.nextInt().toFloat() }
        val compressed = LZ4NativeCompression.compress(original)
        val decompressed = LZ4NativeCompression.decompressFloats(compressed, original.size)

        original.indices.forEach { index ->
            assertThat(decompressed[index].toRawBits(), "Mismatch at index $index")
                .isEqualTo(original[index].toRawBits())
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

    @Test
    fun floatArray_expectedCountByteSizeOverflow_throwsBeforeAllocation() {
        assertThrows(ArithmeticException::class.java) {
            LZ4NativeCompression.decompressFloats(byteArrayOf(), Int.MAX_VALUE)
        }
    }
}
