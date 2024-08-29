package com.eva.recorderapp.voice_recorder.presentation.recorder.util

import com.eva.recorderapp.voice_recorder.domain.recorder.emums.RecorderState

enum class RecorderActionMode {
	INIT,
	RECORDING,
	PREPARING,
}

val RecorderState.toAction: RecorderActionMode
	get() = when (this) {
		RecorderState.IDLE, RecorderState.COMPLETED, RecorderState.CANCELLED -> RecorderActionMode.INIT
		RecorderState.RECORDING, RecorderState.PAUSED -> RecorderActionMode.RECORDING
		else -> RecorderActionMode.PREPARING
	}

val RecorderState.showTopbarActions: Boolean
	get() = this in arrayOf(RecorderState.IDLE, RecorderState.COMPLETED, RecorderState.CANCELLED)