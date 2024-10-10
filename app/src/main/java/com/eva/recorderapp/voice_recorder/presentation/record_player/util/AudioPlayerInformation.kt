package com.eva.recorderapp.voice_recorder.presentation.record_player.util

import com.eva.recorderapp.voice_recorder.domain.player.model.PlayerMetaData
import com.eva.recorderapp.voice_recorder.domain.player.model.PlayerTrackData

data class AudioPlayerInformation(
	val trackData: PlayerTrackData = PlayerTrackData(),
	val playerMetaData: PlayerMetaData = PlayerMetaData(),
)