package com.eva.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface PlayerSubGraph {

	// we need audio id to mark to get the route data from saved state handle
	@Serializable
	data class NavGraph(val audioId: Long) : PlayerSubGraph

	// we need the audio id to let deep links work
	@Serializable
	data class AudioPlayerRoute(val audioId: Long) : PlayerSubGraph

	@Serializable
	data object AudioEditorRoute : PlayerSubGraph
}