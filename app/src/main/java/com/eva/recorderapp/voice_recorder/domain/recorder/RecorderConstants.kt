package com.eva.recorderapp.voice_recorder.domain.recorder

import kotlinx.datetime.LocalTime
import kotlin.time.Duration.Companion.milliseconds

object RecorderConstants {

	// don't change the values
	const val RECORDER_AMPLITUDES_BUFFER_SIZE = 100
	val AMPS_READ_DELAY_RATE = 100.milliseconds

	fun padListWithExtra(list: List<LocalTime>, blockSize: Int, extra: Int = 10)
			: List<LocalTime> {
		val sizeDiff = blockSize - list.size
		val lastValue = list.lastOrNull() ?: LocalTime.fromMillisecondOfDay(0)
		// extra will create the translation effect properly
		val amount = if (sizeDiff >= 0) sizeDiff else 0
		return list + List(amount + extra) {
			val millis =
				lastValue.toMillisecondOfDay() + ((it + 1) * RECORDER_AMPLITUDES_BUFFER_SIZE)
			LocalTime.fromMillisecondOfDay(millis)
		}
	}
}