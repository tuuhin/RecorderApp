package com.eva.feature_player.viewmodel

import dagger.assisted.AssistedFactory

@AssistedFactory
internal interface PlayerViewmodelFactory {

	fun create(fileId: Long): AudioPlayerViewModel
}