package com.eva.feature_player.viewmodel

import com.eva.recordings.domain.models.AudioFileModel
import dagger.assisted.AssistedFactory

@AssistedFactory
internal interface PlayerViewmodelFactory {

	fun create(fileModel: AudioFileModel): AudioPlayerViewModel
}