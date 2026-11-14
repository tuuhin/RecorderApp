package com.eva.recorder_native.models

data class RecorderConfig(
    val sampleRate: Int = 48_000,
	val channel: Int = 1,
)
