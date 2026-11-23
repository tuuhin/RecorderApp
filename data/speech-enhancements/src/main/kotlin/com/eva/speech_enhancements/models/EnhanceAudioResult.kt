package com.eva.speech_enhancements.models

import kotlin.time.Duration
import kotlin.time.DurationUnit

data class EnhanceAudioResult(
    val samples: FloatArray,
    val sampleRate: Int,
    val totalDuration: Duration,
    val processingDuration: Duration,
) {
    val isEmpty: Boolean
        get() = samples.isEmpty()

    /** Processing speed; above 1.0 is faster than real time. */
    val realtimeFactor: Double
        get() {
            val processingInNanos = processingDuration.toDouble(DurationUnit.NANOSECONDS)
            val totalDurationInNanos = totalDuration.toDouble(DurationUnit.NANOSECONDS)
            return if (processingInNanos > 0.0) totalDurationInNanos / processingInNanos else 0.0
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EnhanceAudioResult

        if (sampleRate != other.sampleRate) return false
        if (!samples.contentEquals(other.samples)) return false
        if (totalDuration != other.totalDuration) return false
        if (processingDuration != other.processingDuration) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sampleRate
        result = 31 * result + samples.contentHashCode()
        result = 31 * result + totalDuration.hashCode()
        result = 31 * result + processingDuration.hashCode()
        return result
    }

}
