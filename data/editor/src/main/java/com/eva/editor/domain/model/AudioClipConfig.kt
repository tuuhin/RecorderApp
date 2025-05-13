package com.eva.editor.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class AudioClipConfig(
	val start: Duration = 0.seconds,
	val end: Duration = 1.seconds,
) {
	fun validate(totalDuration: Duration): Boolean {
		return hasMinimumDuration && start.isPositive() && end <= totalDuration
	}

	val hasMinimumDuration: Boolean
		get() = end - start >= MIN_CLIP_DURATION

	companion object {
		val MIN_CLIP_DURATION = 1.seconds
	}
}