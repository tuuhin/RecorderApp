package com.eva.player.data.reader

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

fun FloatArray.lowPassFilter(sampleRate: Int, cutoffFrequency: Float): FloatArray {
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


fun FloatArray.normalize(): FloatArray {

	val maxValue = maxOrNull() ?: .0f
	val minValue = minOrNull() ?: .0f

	if (maxValue == minValue || isEmpty()) return this

	val diff = (maxValue - minValue).let { value -> if (value == 0f) 1f else value }
	val copy = copyOf()
	for (i in 1..<lastIndex) {
		copy[i] = (this[i] - minValue) / diff
	}
	return copy
}


fun FloatArray.smoothen(smoothness: Float = .7f): FloatArray {
	if (isEmpty() || smoothness == 0f) return this
	val original = copyOf()
	for (i in 1..<lastIndex) {
		this[i] = (original[i - 1] * smoothness * 0.5f) +
				(original[i] * (1 - smoothness)) +
				(original[i + 1] * smoothness * 0.5f)
	}
	return this
}