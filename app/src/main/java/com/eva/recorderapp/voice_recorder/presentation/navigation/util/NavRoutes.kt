package com.eva.recorderapp.voice_recorder.presentation.navigation.util

import kotlinx.serialization.Serializable

sealed interface NavRoutes {

	@Serializable
	data object VoiceRecorder : NavRoutes

	@Serializable
	data object VoiceRecordings : NavRoutes
}