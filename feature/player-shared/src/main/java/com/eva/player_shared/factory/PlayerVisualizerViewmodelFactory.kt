package com.eva.player_shared.factory

import com.eva.player_shared.PlayerVisualizerViewmodel
import dagger.assisted.AssistedFactory

@AssistedFactory
interface PlayerVisualizerViewmodelFactory {

    fun create(audioId: Long): PlayerVisualizerViewmodel
}

