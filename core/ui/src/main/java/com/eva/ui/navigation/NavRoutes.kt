package com.eva.ui.navigation

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
	data object AudioSettings : NavRoutes

	@Serializable
	data class CreateOrUpdateCategory(val categoryId: Long? = null) : NavRoutes

	@Serializable
	data class SelectRecordingCategoryRoute(
		val recordingIds: Collection<Long> = emptyList(),
	) : NavRoutes

	@Serializable
	data object SearchRecordings : NavRoutes

}