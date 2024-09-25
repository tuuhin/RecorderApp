package com.eva.recorderapp.voice_recorder.domain.player

import kotlinx.datetime.LocalTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

data class PlayerTrackData(
	private val current: Duration = 0.seconds,
	private val total: Duration = 0.seconds,
) {
	val currentAsLocalTime: LocalTime
		get() = LocalTime.fromMillisecondOfDay(current.toInt(DurationUnit.MILLISECONDS))

	val totalAsLocalTime: LocalTime
		get() = LocalTime.fromSecondOfDay(total.toInt(DurationUnit.SECONDS))

	val playRatio: Float
		get() {
			val totalSeconds = total.toDouble(DurationUnit.MILLISECONDS)
			val playedSeconds = current.toDouble(DurationUnit.MILLISECONDS)
			val ratio = (playedSeconds / totalSeconds).toFloat()
			if (ratio.isNaN() || ratio.isInfinite()) return 0f
			return ratio.coerceIn(0f, 1f)
		}

	fun calculateSeekAmount(seek: Float): Duration {
		try {
			require(value = seek in 0f..1f)

			val amount = if (total.isPositive() && total.isFinite()) {
				val totalInFloat = total.toDouble(DurationUnit.MILLISECONDS)
				val seekAmount = seek * totalInFloat
				seekAmount.coerceIn(0.0, totalInFloat)
			} else 0.0
			return amount.milliseconds

		} catch (e: Exception) {
			e.printStackTrace()
			return 0.milliseconds
		}

	}
}