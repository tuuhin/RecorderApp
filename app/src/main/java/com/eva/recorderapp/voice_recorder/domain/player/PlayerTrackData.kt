package com.eva.recorderapp.voice_recorder.domain.player

import kotlinx.datetime.LocalTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class PlayerTrackData(
	val current: Duration = 0.seconds,
	val total: Duration = 0.seconds,
) {
	val currentAsLocalTime: LocalTime
		get() = LocalTime.fromSecondOfDay(current.inWholeSeconds.toInt())

	val totalAsLocalTime: LocalTime
		get() = LocalTime.fromSecondOfDay(total.inWholeSeconds.toInt())

	val leftDuration: Duration
		get() {
			val newDuration = total.minus(current)
			return if (newDuration.isPositive()) newDuration
			else 0.seconds
		}

	val leftDurationAsLocalTime: LocalTime
		get() = LocalTime.fromSecondOfDay(leftDuration.inWholeSeconds.toInt())

	val playRatio: Float
		get() {
			val totalSeconds = total.inWholeMilliseconds.toFloat()
			val playedSeconds = current.inWholeMilliseconds.toFloat()
			val ratio = playedSeconds / totalSeconds
			if (ratio.isNaN() || ratio.isInfinite()) return 0f
			return ratio.coerceIn(0f, 1f)
		}
}