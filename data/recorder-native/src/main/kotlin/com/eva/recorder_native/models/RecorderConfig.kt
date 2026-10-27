package com.eva.recorder_native.models

data class RecorderConfig(
	val sampleRate: Int = 44_100,
	val channel: Int = 1,
)