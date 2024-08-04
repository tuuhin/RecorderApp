package com.eva.recorderapp.voice_recorder.domain.player

enum class PlayerPlayBackSpeed(val speed: Float) {

	VERY_SLOW(0.25f),
	SLOW(.5f),
	NORMAL(1f),
	FAST(1.25f),
	VERY_FAST(1.5f),
	VERY_VERY_FAST(2f);

	companion object {
		fun fromInt(value: Float): PlayerPlayBackSpeed? {
			return when (value) {
				0.25f -> VERY_SLOW
				.5f -> SLOW
				1f -> NORMAL
				1.25f -> FAST
				1.5f -> VERY_FAST
				2f -> VERY_VERY_FAST
				else -> null
			}
		}
	}
}