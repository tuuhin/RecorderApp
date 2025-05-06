package com.eva.editor.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class AudioClipConfig(
	val start: Duration = 0.seconds,
	val end: Duration = 1.seconds,
) {
	fun validate(audioDuration: Duration): Boolean {
		return end - start >= 1.seconds && start.isPositive() && end <= audioDuration
	}

	val hasMinimumDuration: Boolean
		get() = end - start >= 1.seconds
}