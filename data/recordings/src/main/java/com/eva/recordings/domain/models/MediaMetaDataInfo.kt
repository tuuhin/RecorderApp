package com.eva.recordings.domain.models

data class MediaMetaDataInfo(
	val channelCount: Int = 0,
	val sampleRate: Int = 0,
	val bitRate: Int = 0,
	val locationString: String? = null,
) {
	val sampleRateInKHz: Float
		get() = sampleRate / 1_000f

	val bitRateInKbps
		get() = bitRate / 1_000f
}