package com.eva.player.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

data class PlayerTrackData(
	val current: Duration = 0.seconds,
	val total: Duration = 0.seconds,
) {

	val playRatio: Float
		get() {
			val totalSeconds = total.toDouble(DurationUnit.MILLISECONDS)
			val playedSeconds = current.toDouble(DurationUnit.MILLISECONDS)
			val ratio = (playedSeconds / totalSeconds).toFloat()
			if (ratio.isNaN() || ratio.isInfinite()) return 0f
			return ratio.coerceIn(0f, 1f)
		}

	val allPositiveAndFinite: Boolean
		get() {
			val isCurrentValid = current.isFinite() && current >= 0.seconds
			val isTotalValid = total.isFinite() && total.isPositive()

			return isCurrentValid && isTotalValid
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