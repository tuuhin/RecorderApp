package com.com.visualizer.data

import java.nio.ByteBuffer

private const val TWO_POWER_15 = 32_768f
private const val TWO_POWER_7 = 128f

fun ByteBuffer.asFloatArray(size: Int, pcmEncoding: Int, channelCount: Int): FloatArray {
	val totalSamples = size / (pcmEncoding / 8)
	val samplesPerChannel = totalSamples / channelCount
	val floatArray = FloatArray(samplesPerChannel)
	try {
		when (pcmEncoding) {
			16 -> {
				val shortBuffer = asShortBuffer()
				val shortArray = ShortArray(totalSamples)
				shortBuffer.get(shortArray)

				for (i in 0 until samplesPerChannel) {
					var sum = 0f
					for (channel in 0 until channelCount) {
						sum += shortArray[i * channelCount + channel] / TWO_POWER_15
					}
					floatArray[i] = sum / channelCount
				}
			}

			8 -> {
				val byteArray = ByteArray(size)
				get(byteArray)
				for (i in 0 until samplesPerChannel) {
					var sum = 0f
					for (channel in 0 until channelCount) {
						sum += byteArray[i * channelCount + channel] / TWO_POWER_7
					}
					floatArray[i] = sum / channelCount
				}
			}

			32 -> {
				val floatBuffer = asFloatBuffer()
				val tempFloatArray = FloatArray(totalSamples)
				floatBuffer.get(tempFloatArray)

				for (i in 0 until samplesPerChannel) {
					var sum = 0f
					for (channel in 0 until channelCount) {
						sum += tempFloatArray[i * channelCount + channel]
					}
					floatArray[i] = sum / channelCount
				}
			}

			else -> throw IllegalArgumentException("Unsupported PCM encoding: $pcmEncoding")
		}
	} catch (e: Exception) {
		e.printStackTrace()
	}
	return floatArray
}
