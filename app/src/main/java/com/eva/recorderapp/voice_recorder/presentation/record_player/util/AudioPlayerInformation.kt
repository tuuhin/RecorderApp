package com.eva.recorderapp.voice_recorder.presentation.record_player.util

import androidx.compose.runtime.Stable
import com.eva.recorderapp.voice_recorder.domain.player.PlayerMetaData
import com.eva.recorderapp.voice_recorder.domain.player.PlayerTrackData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalTime

@Stable
data class AudioPlayerInformation(
	val trackData: PlayerTrackData = PlayerTrackData(),
	val playerMetaData: PlayerMetaData = PlayerMetaData(),
	val waveforms: List<Float> = emptyList(),
	val bookmarks: ImmutableList<LocalTime> = persistentListOf(),
)