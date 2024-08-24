package com.eva.recorderapp.voice_recorder.presentation.navigation.util

import kotlinx.serialization.Serializable

sealed interface NavRoutes {

	@Serializable
	data object VoiceRecorder : NavRoutes

	@Serializable
	data object VoiceRecordings : NavRoutes

	@Serializable
	data object TrashRecordings : NavRoutes

	@Serializable
	data class AudioPlayer(val audioId: Long) : NavRoutes

	@Serializable
	data object AudioEditor : NavRoutes

	@Serializable
	data object AudioSettings : NavRoutes

	@Serializable
	data object ApplicationInfo : NavRoutes

	companion object {
		const val AUDIO_PLAYER_PARAM_NAME = "audioId"
	}
}