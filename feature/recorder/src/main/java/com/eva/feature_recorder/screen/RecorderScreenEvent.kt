package com.eva.feature_recorder.screen

sealed interface RecorderScreenEvent {
	data object BindRecorderService : RecorderScreenEvent
	data object UnBindRecorderService : RecorderScreenEvent
}