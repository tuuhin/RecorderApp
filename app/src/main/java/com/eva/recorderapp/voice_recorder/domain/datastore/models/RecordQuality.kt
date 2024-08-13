package com.eva.recorderapp.voice_recorder.domain.datastore.models

/**
 * Recording record quality
 * @param bitRate The number of bits per second that can be recived
 * @param sampleRate The number of samples taken per second
 */
enum class RecordQuality(val bitRate: Int, val sampleRate: Int) {
	HIGH(bitRate = 256_000, sampleRate = 48_000),
	NORMAL(bitRate = 128_000, sampleRate = 44_100),
	LOW(bitRate = 64_000, sampleRate = 44_100),
}