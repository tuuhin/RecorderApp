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
}