package com.com.visualizer.data.ext

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal fun FloatArray.lowPassFilter(sampleRate: Int, cutoffFrequency: Float): FloatArray {
    val cutOff = cutoffFrequency / (sampleRate / 2f)
    val filterLength = 101
    val impulseResponse = FloatArray(filterLength)
    val middle = filterLength / 2
    for (n in 0 until filterLength) {
        impulseResponse[n] = if (n == middle) cutOff
        else (cutOff * sin(PI * cutOff * (n - middle))).toFloat() /
            (PI * (n - middle)).toFloat()
    }

    val windowResponse = FloatArray(filterLength)
    for (n in 0 until filterLength) {
        val someValue = (0.54f - 0.46f * cos(2 * PI * n / (filterLength - 1))).toFloat()
        windowResponse[n] = impulseResponse[n] * someValue
    }

    val output = FloatArray(size)
    for (i in indices) {
        var sum = 0f
        for (j in 0 until filterLength) {
            if (i - j >= 0) {
                sum += get(i - j) * windowResponse[j]
            }
        }
        output[i] = sum
    }
    return output
}

fun FloatArray.compressFloatArray(m: Int): FloatArray {
    if (m <= 0) throw IllegalArgumentException("m must be a positive integer")

    if (isEmpty()) return FloatArray(m)

    val n = size
    if (m >= n) return copyOf()

    val compressed = FloatArray(m)
    val ratio = n.toFloat() / m.toFloat()
    var currentInputIndex = 0f

    for (i in 0 until m) {
        var squaredSum = 0f
        var count = 0
        val segmentStart = currentInputIndex.toInt()
        val segmentEnd = (currentInputIndex + ratio).toInt()

        for (j in segmentStart until segmentEnd) {
            if (j < n) { //important check
                val value = get(j)
                squaredSum += value * value
                count++
            }
        }
        if (count > 0) {
            val rms = sqrt(squaredSum / count)
            compressed[i] = rms // You can choose average or RMS.
        } else compressed[i] = 0f

        currentInputIndex += ratio
    }
    return compressed
}

internal fun FloatArray.resampleRms(sourcePointMs: Int, requestedPointMs: Int): FloatArray {
    require(sourcePointMs > 0 && requestedPointMs > 0)
    if (isEmpty() || sourcePointMs == requestedPointMs) return copyOf()

    val totalMs = size.toLong() * sourcePointMs
    val outputSize = (totalMs + requestedPointMs - 1L) / requestedPointMs
    require(outputSize <= Int.MAX_VALUE) { "Requested waveform contains too many points" }

    return FloatArray(outputSize.toInt()) { index ->
        val start = index.toLong() * requestedPointMs
        val end = minOf(totalMs, start + requestedPointMs)
        var cursor = start
        var sourceIndex = (start / sourcePointMs).toInt()
        var weightedEnergy = 0.0

        while (cursor < end) {
            val next = minOf(end, (sourceIndex + 1L) * sourcePointMs)
            val value = this[sourceIndex].toDouble()
            weightedEnergy += value * value * (next - cursor)
            cursor = next
            sourceIndex++
        }
        sqrt(weightedEnergy / (end - start)).toFloat()
    }
}

internal fun FloatArray.normalize(): FloatArray {

    val maxValue = maxOrNull() ?: .0f
    val minValue = minOrNull() ?: .0f

    if (maxValue == minValue || isEmpty()) return this

    val diff = (maxValue - minValue).let { value -> if (value == 0f) 1f else value }

    for (i in indices) this[i] = (this[i] - minValue) / diff
    return this
}


internal fun FloatArray.smoothen(smoothness: Float = .7f): FloatArray {
    if (size < 3 || smoothness == 0f) return this

    var previous = this[0]
    for (i in 1..<lastIndex) {
        val current = this[i]
        this[i] = (previous * smoothness * 0.5f) + (current * (1 - smoothness)) + (this[i + 1] * smoothness * 0.5f)
        previous = current
    }
    return this
}
