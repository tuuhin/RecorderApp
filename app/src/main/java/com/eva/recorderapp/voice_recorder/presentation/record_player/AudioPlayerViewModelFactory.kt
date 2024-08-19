package com.eva.recorderapp.voice_recorder.presentation.record_player

import dagger.assisted.AssistedFactory

@AssistedFactory
interface AudioPlayerViewModelFactory {

	fun create(audioId: Long): AudioPlayerViewModel
}
