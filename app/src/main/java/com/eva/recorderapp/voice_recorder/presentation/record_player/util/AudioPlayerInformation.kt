package com.eva.recorderapp.voice_recorder.presentation.record_player.util

import com.eva.recorderapp.voice_recorder.domain.player.PlayerMetaData
import com.eva.recorderapp.voice_recorder.domain.player.PlayerTrackData

data class AudioPlayerInformation(
	val trackData: PlayerTrackData = PlayerTrackData(),
	val playerMetaData: PlayerMetaData = PlayerMetaData(),
	val waveforms: PlayerGraphInfo = PlayerGraphInfo()
)