package com.eva.recorderapp.voice_recorder.domain.player

import kotlinx.datetime.LocalTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

data class PlayerTrackData(
	val current: Duration = 0.seconds,
	val total: Duration = 0.seconds,
) {
	val currentAsLocalTime: LocalTime
		get() = LocalTime.fromMillisecondOfDay(current.toInt(DurationUnit.MILLISECONDS))

	val totalAsLocalTime: LocalTime
		get() = LocalTime.fromSecondOfDay(total.inWholeSeconds.toInt())

	private val leftDuration: Duration
		get() {
			val newDuration = total.minus(current)
			return if (newDuration.isPositive()) newDuration
			else 0.seconds
		}

	val leftDurationAsLocalTime: LocalTime
		get() = LocalTime.fromSecondOfDay(leftDuration.inWholeSeconds.toInt())

	val playRatio: Float
		get() {
			val totalSeconds = total.toDouble(DurationUnit.MILLISECONDS)
			val playedSeconds = current.toDouble(DurationUnit.MILLISECONDS)
			val ratio = (playedSeconds / totalSeconds).toFloat()
			if (ratio.isNaN() || ratio.isInfinite()) return 0f
			return ratio.coerceIn(0f, 1f)
		}

	fun calculateSeekAmount(seek: Float): Double {

		require(value = seek in 0f..1f)

		return if (total.isPositive() && total.isFinite()) {
			val totalInFloat = total.toDouble(DurationUnit.MILLISECONDS)
			val seekAmount = seek * totalInFloat
			val amt = seekAmount.coerceIn(0.0, totalInFloat)
			return amt
		} else 0.0
	}
}