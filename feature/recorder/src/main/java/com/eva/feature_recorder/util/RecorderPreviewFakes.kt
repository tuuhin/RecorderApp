package com.eva.feature_recorder.util

import com.eva.utils.RecorderConstants
import kotlinx.datetime.LocalTime
import kotlin.random.Random

internal object RecorderPreviewFakes {

	private val PREVIEW_RECORDER_AMPLITUDE_FLOAT_ARRAY_LARGE = List(150) {
		Random.nextFloat()
	}.mapIndexed { idx, amp ->
		val duration = RecorderConstants.AMPS_READ_DELAY_RATE.times(idx)
		val time = LocalTime.fromMillisecondOfDay(duration.inWholeMilliseconds.toInt())
		time to amp
	}

	val PREVIEW_RECORDER_AMPLITUDES_FLOAT_ARRAY =
		PREVIEW_RECORDER_AMPLITUDE_FLOAT_ARRAY_LARGE.take(100)
}