package com.eva.recorderapp.voice_recorder.presentation.record_player.util

import com.eva.recorderapp.voice_recorder.domain.player.PlayerMetaData
import com.eva.recorderapp.voice_recorder.domain.player.PlayerTrackData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class AudioPlayerInformation(
	val trackData: PlayerTrackData = PlayerTrackData(),
	val playerMetaData: PlayerMetaData = PlayerMetaData(),
	val sampling: ImmutableList<Float> = persistentListOf()
)