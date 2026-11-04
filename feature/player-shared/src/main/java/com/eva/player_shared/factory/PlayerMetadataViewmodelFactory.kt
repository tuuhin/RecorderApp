package com.eva.player_shared.factory

import com.eva.player_shared.PlayerMetadataViewmodel
import dagger.assisted.AssistedFactory

@AssistedFactory
interface PlayerMetadataViewmodelFactory {

    fun create(audioId: Long): PlayerMetadataViewmodel
}

