package com.eva.editor.data

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class AudioClipConfig(
	val start: Duration = 0.seconds,
	val end: Duration = 1.seconds,
) {
	fun validate(): Boolean {
		val diff = end - start
		return diff >= 1.seconds
	}
}