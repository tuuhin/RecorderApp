package com.eva.recordings.domain.models

data class DeviceTotalStorageModel(
	val totalAmountInBytes: Long = 0L,
	val freeAmountInBytes: Long = 0L
) {

	val usedSpaceInBytes: Long
		get() = (totalAmountInBytes - freeAmountInBytes)
			.coerceIn(0..totalAmountInBytes)

	val usedSpacePercentage: Float
		get() {
			val ratio = if (totalAmountInBytes == 0L) 0f
			else (usedSpaceInBytes.toFloat() / totalAmountInBytes)
			return ratio.coerceIn(0f..1f)
		}
}