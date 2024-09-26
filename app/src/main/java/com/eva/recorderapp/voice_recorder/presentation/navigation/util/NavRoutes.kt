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
	data object ManageCategories : NavRoutes

	@Serializable
	data class AudioPlayer(val audioId: Long) : NavRoutes

	@Serializable
	data object AudioEditor : NavRoutes

	@Serializable
	data object AudioSettings : NavRoutes

	@Serializable
	data class CreateOrUpdateCategory(val categoryId: Long? = null) : NavRoutes

	@Serializable
	data class SelectRecordingCategoryRoute(
		val recordingIds: Collection<Long> = emptyList(),
	) : NavRoutes

}