package com.eva.feature_recorder.util

import com.eva.recorder.domain.models.RecorderState

internal enum class RecorderActionMode {
	INIT,
	RECORDING,
	PREPARING,
}

internal val RecorderState.toAction: RecorderActionMode
	get() = when (this) {
		RecorderState.IDLE, RecorderState.COMPLETED, RecorderState.CANCELLED -> RecorderActionMode.INIT
		RecorderState.RECORDING, RecorderState.PAUSED -> RecorderActionMode.RECORDING
		else -> RecorderActionMode.PREPARING
	}

internal val RecorderState.showTopBarActions: Boolean
	get() = this in arrayOf(RecorderState.IDLE, RecorderState.COMPLETED, RecorderState.CANCELLED)