package com.eva.player.domain.model

sealed class PlayerPlayBackSpeed(val speed: Float) {

	data object VerySlow : PlayerPlayBackSpeed(.25f)
	data object Slow : PlayerPlayBackSpeed(.5f)
	data object Normal : PlayerPlayBackSpeed(1f)
	data object Fast : PlayerPlayBackSpeed(1.25f)
	data object VeryFast : PlayerPlayBackSpeed(1.5f)
	data object VeryVeryFast : PlayerPlayBackSpeed(2f)
	data class CustomSpeed(val amount: Float) : PlayerPlayBackSpeed(amount.coerceIn(SPEED_RANGE))

	companion object {
		private val SPEED_RANGE = 0f..2f

		fun fromInt(value: Float): PlayerPlayBackSpeed? {
			return when (value) {
				0.25f -> VerySlow
				.5f -> Slow
				1f -> Normal
				1.25f -> Fast
				1.5f -> VeryFast
				2f -> VeryVeryFast
				else -> CustomSpeed(value)
			}
		}
	}
}