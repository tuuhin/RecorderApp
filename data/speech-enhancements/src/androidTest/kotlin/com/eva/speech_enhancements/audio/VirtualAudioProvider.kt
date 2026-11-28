package com.eva.speech_enhancements.audio

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin


object VirtualAudioProvider {

    const val SAMPLE_RATE = 48_000
    const val BYTES_PER_SAMPLE = 2
    private const val HEADER_SIZE = 44


    fun noisySpeechClip(parentDir: File, seconds: Int = 2): NoisySpeechClip {
        val sampleCount = SAMPLE_RATE * seconds
        val clean = FloatArray(sampleCount) { index ->
            val t = index.toDouble()
            val speech = 0.30 * sin(2.0 * PI * 220.0 * t / SAMPLE_RATE) +
                0.20 * sin(2.0 * PI * 880.0 * t / SAMPLE_RATE) +
                0.10 * sin(2.0 * PI * 2400.0 * t / SAMPLE_RATE)
            val envelope = 0.5 * (1.0 + sin(2.0 * PI * 0.5 * t / SAMPLE_RATE))
            (speech * envelope).toFloat()
        }
        val noisy = FloatArray(sampleCount) { index ->
            (clean[index] + 0.05 * (2.0 * deterministicNoise(index) - 1.0)).toFloat()
        }
        val file = writeWav(File(parentDir, "virtual_noisy_speech.wav"), noisy)
        return NoisySpeechClip(file, noisy, clean)
    }

    fun alternatingLoudSilenceClip(parentDir: File, cycles: Int = 2, segmentSeconds: Int = 1): File {
        val samplesPerSegment = SAMPLE_RATE * segmentSeconds
        val pcm = ShortArray(samplesPerSegment * 2 * cycles)
        var index = 0
        repeat(cycles) {
            for (i in 0 until samplesPerSegment) {
                val angle = 2.0 * PI * 440.0 * i / SAMPLE_RATE
                pcm[index++] = (sin(angle) * 0.9 * Short.MAX_VALUE).toInt().toShort()
            }
            repeat(samplesPerSegment) { pcm[index++] = 0 }
        }
        return writeWav(File(parentDir, "virtual_loud_silence.wav"), pcm16ToFloat(pcm))
    }

    fun toneSweepClip(
        parentDir: File,
        seconds: Int = 2,
        startHz: Double = 100.0,
        endHz: Double = 4000.0,
    ): File {
        val sampleCount = SAMPLE_RATE * seconds
        val samples = FloatArray(sampleCount) { index ->
            val progress = index.toDouble() / sampleCount
            val frequency = startHz + (endHz - startHz) * progress
            val angle = 2.0 * PI * frequency * index / SAMPLE_RATE
            (sin(angle) * 0.8).toFloat()
        }
        return writeWav(File(parentDir, "virtual_tone_sweep.wav"), samples)
    }

    fun readWav(file: File): FloatArray = readWav(file, 0L)

    private fun readWav(file: File, skipSamples: Long): FloatArray {
        val bytes = file.readBytes()
        check(bytes.size > HEADER_SIZE) { "WAV file is truncated: ${file.path}" }

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        check(String(bytes, 0, 4) == "RIFF" && String(bytes, 8, 4) == "WAVE") {
            "not a RIFF/WAVE file: ${file.path}"
        }

        var offset = 12
        var dataSize = -1
        while (offset + 8 <= bytes.size) {
            val chunkId = String(bytes, offset, 4)
            val chunkSize = readLittleEndianUInt(buffer, offset + 4)
            if (chunkId == "data") {
                dataSize = chunkSize
                break
            }
            offset += 8 + chunkSize
        }
        check(dataSize > 0) { "WAV file has no data chunk: ${file.path}" }

        val totalSamples = dataSize / BYTES_PER_SAMPLE
        val skipped = minOf(skipSamples, totalSamples.toLong()).toInt()
        val out = FloatArray(totalSamples - skipped)
        var index = 0
        for (sampleIndex in skipped until totalSamples) {
            buffer.position(HEADER_SIZE + sampleIndex * BYTES_PER_SAMPLE)
            out[index++] = (buffer.short.toFloat() / Short.MAX_VALUE).coerceIn(-1.0f, 1.0f)
        }
        return out
    }

    fun writeWav(destination: File, samples: FloatArray): File {
        val dataSize = samples.size * BYTES_PER_SAMPLE
        val buffer = ByteBuffer
            .allocate(HEADER_SIZE + dataSize)
            .order(ByteOrder.LITTLE_ENDIAN)

        buffer.put("RIFF".toByteArray())
        buffer.putInt(36 + dataSize)
        buffer.put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16) // PCM chunk size
        buffer.putShort(1) // audio format: PCM
        buffer.putShort(1) // channels: mono
        buffer.putInt(SAMPLE_RATE)
        buffer.putInt(SAMPLE_RATE * BYTES_PER_SAMPLE) // byte rate
        buffer.putShort(BYTES_PER_SAMPLE.toShort()) // block align
        buffer.putShort(16) // bits per sample
        buffer.put("data".toByteArray())
        buffer.putInt(dataSize)

        samples.forEach { sample ->
            val clamped = sample.coerceIn(-1.0f, 1.0f)
            buffer.putShort((clamped * Short.MAX_VALUE).toInt().toShort())
        }

        destination.parentFile?.mkdirs()
        destination.writeBytes(buffer.array())
        return destination
    }

    private fun readLittleEndianUInt(buffer: ByteBuffer, offset: Int): Int {
        buffer.position(offset)
        return buffer.int
    }

    internal fun pcm16ToFloat(pcm: ShortArray): FloatArray =
        FloatArray(pcm.size) { pcm[it].toFloat() / Short.MAX_VALUE }

    private fun deterministicNoise(index: Int): Double {
        var value = (index + 1) * 2654435761L
        value = value xor (value ushr 13)
        value *= 1_547_858_496_639_878_619L
        value = value xor (value ushr 16)
        return ((value % 10_007L) / 10_007.0).coerceIn(0.0, 1.0)
    }
}
