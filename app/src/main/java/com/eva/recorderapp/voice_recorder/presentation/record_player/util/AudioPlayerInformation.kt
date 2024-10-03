package com.eva.recorderapp.voice_recorder.presentation.record_player.util

import androidx.compose.runtime.Stable
import com.eva.recorderapp.voice_recorder.domain.bookmarks.AudioBookmarkModel
import com.eva.recorderapp.voice_recorder.domain.player.model.PlayerMetaData
import com.eva.recorderapp.voice_recorder.domain.player.model.PlayerTrackData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.LocalTime

@Stable
data class AudioPlayerInformation(
	val trackData: PlayerTrackData = PlayerTrackData(),
	val playerMetaData: PlayerMetaData = PlayerMetaData(),
	val bookmarks: ImmutableList<AudioBookmarkModel> = persistentListOf(),
) {

	val bookmarksTimestamps: ImmutableList<LocalTime>
		get() = bookmarks.map(AudioBookmarkModel::timeStamp).toImmutableList()
}