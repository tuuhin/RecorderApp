package com.eva.recorderapp.voice_recorder.presentation.record_player

import com.eva.recorderapp.voice_recorder.domain.player.AudioFilePlayer
import dagger.assisted.AssistedFactory

@AssistedFactory
interface AudioPlayerViewModelFactory {
	fun create(player: AudioFilePlayer, audioId: Long): AudioPlayerViewModel
}
