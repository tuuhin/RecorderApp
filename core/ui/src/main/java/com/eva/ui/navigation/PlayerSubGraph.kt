package com.eva.ui.navigation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface PlayerSubGraph {

    // nav graph is the point this is a new graph
    @Serializable
    data object NavGraph : PlayerSubGraph

    // we need the audio id to let deep links work
    @Serializable
    data class AudioPlayerRoute(
        @SerialName("audioId") val audioId: Long,
        @SerialName("actions") val appActionsEnabled: Boolean = false,
    ) :
        PlayerSubGraph

    // we are using the audio id to set up the player again so audio id
    @Serializable
    data class AudioEditorRoute(@SerialName("audioId") val audioId: Long) : PlayerSubGraph
}
