package com.eva.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface PlayerSubGraph {

	@Serializable
	data object AudioPlayerRoute : PlayerSubGraph

	@Serializable
	data object AudioEditorRoute: PlayerSubGraph
}