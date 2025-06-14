package com.eva.feature_recorder.util

import com.eva.utils.RecorderConstants
import kotlin.random.Random

internal object RecorderPreviewFakes {

	private val PREVIEW_RECORDER_AMPLITUDE_FLOAT_ARRAY_LARGE = List(150) {
		Random.nextFloat()
	}.mapIndexed { idx, amp ->
		RecorderConstants.AMPS_READ_DELAY_RATE.times(idx) to amp
	}

	val PREVIEW_RECORDER_AMPLITUDES_FLOAT_ARRAY =
		PREVIEW_RECORDER_AMPLITUDE_FLOAT_ARRAY_LARGE.take(100)
}